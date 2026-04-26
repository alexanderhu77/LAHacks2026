"""
Precompute CLIP text embeddings for medical-finding labels and write them to
app/src/main/assets/clip_labels.json.

Run once on a laptop (NOT on-device) whenever LABELS changes:

    python -m venv .venv && source .venv/bin/activate
    pip install torch transformers
    python scripts/build_clip_labels.py

The output JSON is consumed by ClipLabelClassifier on Android. Image embeddings
from Zetic's on-device CLIP encoder are compared against these vectors via
cosine similarity to pick the closest medical finding.

Robustness strategy: each medical category has MULTIPLE phrasings. At runtime
we aggregate (mean) the scores within a category, so a scrappy phone photo that
matches one phrasing weakly will still route correctly because the other
phrasings in the same category will pile on.

The "junk" category catches blurry / off-topic / non-medical images so we can
suppress the visual-finding line and tell the LLM the image was unreadable
rather than feed it a confident wrong label.
"""

import json
import os
import sys
from pathlib import Path

try:
    import torch
    from transformers import CLIPModel, CLIPProcessor
except ImportError:
    print("Missing deps. Run: pip install torch transformers", file=sys.stderr)
    sys.exit(1)

# (category, list of phrasings). The category is what we surface to the LLM;
# the phrasings are CLIP-friendly captions averaged together for robustness.
LABELS: list[tuple[str, list[str]]] = [
    ("healthy skin", [
        "a photo of healthy skin",
        "a close-up of normal unblemished skin",
        "a photograph of clear skin with no visible issues",
    ]),
    ("mild rash", [
        "a photo of a mild rash",
        "a close-up of skin with small red bumps",
        "a photograph of slightly irritated red skin",
        "skin with a light pink rash",
    ]),
    ("severe rash with blistering", [
        "a photo of a severe rash with blistering",
        "a close-up of inflamed skin with fluid-filled blisters",
        "a photograph of weeping infected-looking skin",
        "skin with raised blisters and oozing",
    ]),
    ("small superficial cut", [
        "a photo of a small superficial cut",
        "a close-up of a minor scrape on skin",
        "a photograph of a shallow cut with light bleeding",
    ]),
    ("deep cut needing stitches", [
        "a photo of a deep cut with visible tissue",
        "a close-up of a gaping wound that needs stitches",
        "a photograph of a deep laceration with heavy bleeding",
    ]),
    ("infected wound", [
        "a photo of an infected wound with pus",
        "a close-up of a swollen red wound oozing pus",
        "a photograph of a wound surrounded by red inflamed skin",
    ]),
    ("bruise", [
        "a photo of a bruise on skin",
        "a close-up of a purple and yellow bruise",
        "a photograph of a contusion on the body",
    ]),
    ("burn with blistering", [
        "a photo of a burn with blisters",
        "a close-up of a second-degree burn on skin",
        "a photograph of red burned skin with fluid blisters",
    ]),
    ("red swollen eye", [
        "a photo of a swollen red eye",
        "a close-up of an eye with conjunctivitis",
        "a photograph of pink eye with discharge",
    ]),
    ("normal mole", [
        "a photo of a normal round mole on skin",
        "a close-up of a small even-colored mole",
    ]),
    ("irregular mole", [
        "a photo of an irregular asymmetric mole",
        "a close-up of a mole with uneven borders and varied color",
        "a photograph of a suspicious-looking mole",
    ]),
    # Negative class — catches blurry / off-topic / non-medical photos.
    # If "junk" wins, the runtime suppresses the visual-finding line.
    ("junk", [
        "a blurry photograph",
        "an out of focus image",
        "a photo of an unrelated object like furniture",
        "a screenshot of text on a screen",
        "a dark or featureless image",
    ]),
]

OUT_PATH = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "assets" / "clip_labels.json"


def main() -> None:
    print("Loading CLIP model (openai/clip-vit-base-patch32)...")
    model = CLIPModel.from_pretrained("openai/clip-vit-base-patch32")
    proc = CLIPProcessor.from_pretrained("openai/clip-vit-base-patch32")
    model.eval()

    out: list[dict] = []
    with torch.no_grad():
        for category, phrasings in LABELS:
            inputs = proc(text=phrasings, return_tensors="pt", padding=True)
            emb = model.get_text_features(**inputs)
            emb = emb / emb.norm(dim=-1, keepdim=True)  # L2-normalize each phrasing
            out.append({
                "category": category,
                "embeddings": [e.tolist() for e in emb],
                "phrasings": phrasings,
            })
            print(f"  encoded {category}: {len(phrasings)} phrasings")

    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    with open(OUT_PATH, "w") as f:
        json.dump(out, f)
    size_kb = os.path.getsize(OUT_PATH) / 1024
    print(f"\nWrote {len(out)} categories to {OUT_PATH} ({size_kb:.1f} KB)")


if __name__ == "__main__":
    main()

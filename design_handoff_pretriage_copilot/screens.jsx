// screens.jsx — all screens for the pre-triage prototype.
// Uses window.theme (T) — provided by app.jsx via React context-free prop drilling.

const { useState, useEffect, useRef } = React;

// ─── shared atoms ────────────────────────────────────────────

const ScreenShell = ({ T, children, scroll = false, pad = true, bg }) => (
  <div style={{
    flex: 1, display: 'flex', flexDirection: 'column',
    background: bg || T.bg, color: T.ink,
    fontFamily: T.fontBody, fontSize: T.bodySize, lineHeight: 1.45,
    overflow: scroll ? 'auto' : 'hidden',
    padding: pad ? '8px 24px 16px' : 0,
    boxSizing: 'border-box',
  }}>
    {children}
  </div>
);

const PrivacyBadge = ({ T, compact = false }) => (
  <div style={{
    display: 'inline-flex', alignItems: 'center', gap: 8,
    padding: compact ? '6px 10px' : '8px 12px',
    borderRadius: 999,
    background: T.accentSoft, color: T.accentInk,
    fontSize: 13, fontWeight: 500,
    border: `1px solid ${T.accent}22`,
  }}>
    <IconLock size={14} stroke={T.accentInk} sw={2} />
    <span>On-device · nothing leaves your phone</span>
  </div>
);

const Btn = ({ T, children, kind = 'primary', onClick, icon, full = true, big = false }) => {
  const styles = {
    primary: { bg: T.accent, fg: T.chrome === 'dark' ? T.accentInk : '#fff', border: 'transparent' },
    secondary: { bg: 'transparent', fg: T.ink, border: T.border },
    ghost: { bg: 'transparent', fg: T.inkSoft, border: 'transparent' },
    danger: { bg: T.statusRed, fg: '#fff', border: 'transparent' },
  }[kind];
  return (
    <button onClick={onClick} style={{
      width: full ? '100%' : 'auto',
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 10,
      padding: big ? '18px 22px' : '14px 18px',
      borderRadius: 14,
      background: styles.bg, color: styles.fg,
      border: `1px solid ${styles.border}`,
      fontSize: big ? 18 : 16, fontWeight: 600,
      fontFamily: T.fontBody,
      cursor: 'pointer',
      boxShadow: kind === 'primary' ? '0 1px 0 rgba(0,0,0,0.04), 0 8px 24px -12px rgba(0,0,0,0.25)' : 'none',
    }}>
      {icon}
      {children}
    </button>
  );
};

const Display = ({ T, children, size = 28, weight = 500 }) => (
  <div style={{
    fontFamily: T.fontDisplay, fontWeight: weight, fontSize: size,
    lineHeight: 1.15, letterSpacing: '-0.01em', color: T.ink,
    textWrap: 'pretty',
  }}>{children}</div>
);

// ─── 1. SPLASH (model warmup) ────────────────────────────────

function SplashScreen({ T, onDone }) {
  const steps = [
    { name: 'Loading nurse model', detail: 'MedGemma 1.5 · 4B' },
    { name: 'Loading voice model', detail: 'Whisper · tiny' },
    { name: 'Loading privacy filter', detail: 'tanaos anonymizer' },
  ];
  const [i, setI] = useState(0);
  useEffect(() => {
    if (i >= steps.length) { const t = setTimeout(onDone, 600); return () => clearTimeout(t); }
    const t = setTimeout(() => setI(i + 1), 850);
    return () => clearTimeout(t);
  }, [i]);

  const progress = Math.min(1, (i + (i >= steps.length ? 0 : 0.5)) / steps.length);

  return (
    <ScreenShell T={T} pad={false}>
      <div style={{
        flex: 1, display: 'flex', flexDirection: 'column',
        alignItems: 'center', justifyContent: 'center', gap: 28, padding: 28,
      }}>
        <div style={{ position: 'relative', width: 96, height: 96 }}>
          <div style={{
            position: 'absolute', inset: 0, borderRadius: 28,
            background: T.accentSoft,
            animation: 'pulse 2.4s ease-in-out infinite',
          }} />
          <div style={{ position: 'absolute', inset: 12 }}>
            <BrandMark size={72} color={T.accent} bg={T.surface} />
          </div>
        </div>
        <div style={{ textAlign: 'center', maxWidth: 280 }}>
          <Display T={T} size={30}>Nora</Display>
          <div style={{ fontSize: 15, color: T.inkSoft, marginTop: 6 }}>
            Your pre-triage co-pilot
          </div>
        </div>
        <div style={{ width: '100%', maxWidth: 280, marginTop: 12 }}>
          <div style={{
            height: 4, borderRadius: 99, background: T.surfaceAlt, overflow: 'hidden',
          }}>
            <div style={{
              height: '100%', width: `${progress * 100}%`,
              background: T.accent, transition: 'width 600ms ease',
            }} />
          </div>
          <div style={{ marginTop: 16, display: 'flex', flexDirection: 'column', gap: 8 }}>
            {steps.map((s, idx) => (
              <div key={idx} style={{
                display: 'flex', alignItems: 'center', gap: 10,
                fontSize: 14, color: idx < i ? T.ink : T.inkMuted,
                opacity: idx <= i ? 1 : 0.55,
              }}>
                <div style={{
                  width: 16, height: 16, borderRadius: 99,
                  display: 'grid', placeItems: 'center',
                  background: idx < i ? T.accent : T.surfaceAlt,
                }}>
                  {idx < i && <IconCheck size={11} stroke="#fff" sw={3} />}
                </div>
                <span style={{ flex: 1 }}>{s.name}</span>
                <span style={{ fontFamily: T.fontMono, fontSize: 11, color: T.inkMuted }}>{s.detail}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
      <div style={{ padding: 20, display: 'flex', justifyContent: 'center' }}>
        <PrivacyBadge T={T} compact />
      </div>
      <style>{`
        @keyframes pulse { 0%,100% { transform: scale(1); opacity: .6 } 50% { transform: scale(1.15); opacity: .25 } }
      `}</style>
    </ScreenShell>
  );
}

// ─── 2. PERMISSIONS ──────────────────────────────────────────

function PermissionsScreen({ T, onAllow, onSkip }) {
  return (
    <ScreenShell T={T}>
      <div style={{ padding: '24px 4px 16px' }}>
        <BrandMark size={44} color={T.accent} bg={T.accentSoft} />
        <div style={{ height: 18 }} />
        <Display T={T} size={28}>One quick step before we start</Display>
        <div style={{ color: T.inkSoft, marginTop: 10, fontSize: 16 }}>
          Voice and camera make Nora more accessible. You can use text instead — your choice, every time.
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 8 }}>
        {[
          { icon: <IconMic size={20} stroke={T.accent} />, title: 'Microphone', body: "So you can speak symptoms instead of typing." },
          { icon: <IconCamera size={20} stroke={T.accent} />, title: 'Camera', body: "Optional — for rashes, wounds, or moles." },
        ].map((p, i) => (
          <div key={i} style={{
            display: 'flex', gap: 14, padding: 16,
            background: T.surface, border: `1px solid ${T.border}`, borderRadius: 16,
          }}>
            <div style={{
              width: 40, height: 40, borderRadius: 12, background: T.accentSoft,
              display: 'grid', placeItems: 'center', flexShrink: 0,
            }}>{p.icon}</div>
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 600, fontSize: 16 }}>{p.title}</div>
              <div style={{ color: T.inkSoft, fontSize: 14, marginTop: 2 }}>{p.body}</div>
            </div>
          </div>
        ))}
      </div>

      <div style={{ flex: 1 }} />

      <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 20 }}>
        <PrivacyBadge T={T} />
        <Btn T={T} onClick={onAllow} big>Allow & continue</Btn>
        <Btn T={T} kind="ghost" onClick={onSkip}>Use text only</Btn>
      </div>
    </ScreenShell>
  );
}

// ─── 3. INTAKE (voice-first) ─────────────────────────────────

function IntakeScreen({ T, onSubmit, presetTranscript }) {
  const [recording, setRecording] = useState(false);
  const [transcript, setTranscript] = useState(presetTranscript || '');
  const [showText, setShowText] = useState(false);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    if (!recording) return;
    const id = setInterval(() => setTick(t => t + 1), 100);
    return () => clearInterval(id);
  }, [recording]);

  // Demo: progressively transcribe
  useEffect(() => {
    if (!recording) return;
    const phrases = [
      "I've had a",
      "I've had a sore throat",
      "I've had a sore throat for three days",
      "I've had a sore throat for three days and now my ear",
      "I've had a sore throat for three days and now my ear is hurting too.",
    ];
    const id = setTimeout(() => {
      const idx = Math.min(phrases.length - 1, Math.floor(tick / 6));
      setTranscript(phrases[idx]);
      if (idx === phrases.length - 1) setRecording(false);
    }, 0);
    return () => clearTimeout(id);
  }, [tick, recording]);

  const bars = Array.from({ length: 28 }, (_, i) => {
    const amp = recording
      ? 0.3 + 0.7 * Math.abs(Math.sin((tick + i * 3) * 0.4) * Math.cos(i * 0.7))
      : 0.18;
    return amp;
  });

  return (
    <ScreenShell T={T}>
      {/* Header */}
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '12px 0 8px',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <BrandMark size={32} color={T.accent} bg={T.accentSoft} />
          <div>
            <div style={{ fontWeight: 600, fontSize: 15 }}>Nora</div>
            <div style={{ fontSize: 12, color: T.inkMuted }}>Pre-triage co-pilot</div>
          </div>
        </div>
        <PrivacyBadge T={T} compact />
      </div>

      {/* Prompt */}
      <div style={{ marginTop: 22 }}>
        <Display T={T} size={30}>What's going on today?</Display>
        <div style={{ color: T.inkSoft, marginTop: 8, fontSize: 16 }}>
          Tap and tell me in your own words — when it started, where it hurts, anything else.
        </div>
      </div>

      {/* Transcript */}
      <div style={{
        marginTop: 24, minHeight: 96,
        padding: 16, borderRadius: 16,
        background: transcript ? T.surface : 'transparent',
        border: transcript ? `1px solid ${T.border}` : `1px dashed ${T.border}`,
        color: transcript ? T.ink : T.inkMuted,
        fontSize: 17, lineHeight: 1.5,
        fontStyle: transcript ? 'normal' : 'italic',
      }}>
        {transcript || (recording ? 'Listening…' : 'Your words will appear here.')}
        {recording && transcript && (
          <span style={{ display: 'inline-block', width: 2, height: 18, marginLeft: 2,
            background: T.accent, animation: 'blink 1s infinite', verticalAlign: 'middle' }} />
        )}
      </div>

      {/* Waveform */}
      <div style={{
        marginTop: 24, height: 54,
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 3,
      }}>
        {bars.map((amp, i) => (
          <div key={i} style={{
            width: 4, height: `${amp * 100}%`,
            borderRadius: 2,
            background: recording ? T.accent : T.border,
            transition: 'height 100ms ease',
          }} />
        ))}
      </div>

      <div style={{ flex: 1 }} />

      {/* Mic button */}
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10, marginTop: 8 }}>
        <button onClick={() => { setRecording(r => !r); if (!recording) setTick(0); }} style={{
          position: 'relative',
          width: 116, height: 116, borderRadius: '50%',
          background: recording ? T.statusRed : T.accent,
          color: '#fff', border: 'none', cursor: 'pointer',
          display: 'grid', placeItems: 'center',
          boxShadow: `0 8px 30px -8px ${recording ? T.statusRed : T.accent}88`,
        }}>
          {recording && (
            <div style={{
              position: 'absolute', inset: -10, borderRadius: '50%',
              border: `2px solid ${T.statusRed}55`,
              animation: 'ring 1.4s ease-out infinite',
            }} />
          )}
          {recording
            ? <div style={{ width: 28, height: 28, borderRadius: 6, background: '#fff' }} />
            : <IconMic size={44} stroke="#fff" sw={1.5} />
          }
        </button>
        <div style={{ fontSize: 14, color: T.inkSoft, fontWeight: 500 }}>
          {recording ? 'Tap to stop' : transcript ? 'Tap to add more' : 'Tap to speak'}
        </div>
      </div>

      {/* Secondary actions */}
      <div style={{ display: 'flex', gap: 10, marginTop: 16 }}>
        <Btn T={T} kind="secondary" onClick={() => setShowText(true)} icon={<IconKeyboard size={18} stroke={T.ink} />}>Type instead</Btn>
        <Btn T={T} onClick={() => onSubmit(transcript)} icon={<IconChevron size={18} stroke="#fff" />}>Continue</Btn>
      </div>

      {showText && (
        <TypeOverlay T={T} value={transcript} onClose={() => setShowText(false)}
          onSave={v => { setTranscript(v); setShowText(false); }} />
      )}

      <style>{`
        @keyframes ring { 0% { transform: scale(1); opacity: .8 } 100% { transform: scale(1.3); opacity: 0 } }
        @keyframes blink { 0%,50% { opacity: 1 } 51%,100% { opacity: 0 } }
      `}</style>
    </ScreenShell>
  );
}

function TypeOverlay({ T, value, onClose, onSave }) {
  const [v, setV] = useState(value);
  return (
    <div style={{
      position: 'absolute', inset: 0, background: T.bg, zIndex: 10,
      display: 'flex', flexDirection: 'column', padding: '16px 24px',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
        <button onClick={onClose} style={{ background: 'none', border: 'none', cursor: 'pointer', color: T.ink, padding: 0 }}>
          <IconClose size={26} stroke={T.ink} />
        </button>
        <div style={{ fontWeight: 600 }}>Type your symptoms</div>
        <button onClick={() => onSave(v)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: T.accent, fontWeight: 600, fontSize: 16 }}>Done</button>
      </div>
      <textarea value={v} onChange={e => setV(e.target.value)} autoFocus
        placeholder="What are you experiencing?"
        style={{
          flex: 1, border: `1px solid ${T.border}`, borderRadius: 14,
          padding: 16, fontSize: 17, fontFamily: T.fontBody,
          background: T.surface, color: T.ink, resize: 'none', outline: 'none',
        }} />
    </div>
  );
}

// ─── 4. CAMERA OFFER ─────────────────────────────────────────

function CameraOfferScreen({ T, onSkip, onCapture }) {
  return (
    <ScreenShell T={T}>
      <div style={{ padding: '24px 4px 8px' }}>
        <div style={{ fontSize: 13, color: T.inkMuted, fontWeight: 600, letterSpacing: 0.4, textTransform: 'uppercase' }}>Optional</div>
        <Display T={T} size={28}>Want to show me?</Display>
        <div style={{ color: T.inkSoft, marginTop: 10, fontSize: 16 }}>
          A photo helps me look at rashes, wounds, eyes, or moles. Skip if it doesn't apply.
        </div>
      </div>

      <div style={{
        marginTop: 24, padding: 18, borderRadius: 18,
        background: T.surface, border: `1px solid ${T.border}`,
      }}>
        <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginBottom: 12 }}>
          <IconShield size={18} stroke={T.accent} />
          <div style={{ fontSize: 14, color: T.inkSoft }}>The image stays on your device.</div>
        </div>
        <div style={{ fontSize: 12, fontWeight: 600, letterSpacing: 0.5, textTransform: 'uppercase', color: T.inkMuted, marginBottom: 6 }}>
          Helpful for things like
        </div>
        <div style={{ fontSize: 15, color: T.inkSoft, lineHeight: 1.6 }}>
          skin or rashes · eyes · wounds · moles
        </div>
      </div>

      <div style={{ flex: 1 }} />

      <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 16 }}>
        <Btn T={T} onClick={onCapture} big icon={<IconCamera size={20} stroke="#fff" />}>Take a photo</Btn>
        <Btn T={T} kind="secondary" onClick={onSkip}>Skip — no photo needed</Btn>
      </div>
    </ScreenShell>
  );
}

// ─── 5. CAMERA CAPTURE / PRECHECK ────────────────────────────

function CameraCaptureScreen({ T, onCancel, onAccept }) {
  const [phase, setPhase] = useState('framing'); // framing | precheck | warned
  const [shutter, setShutter] = useState(false);

  const capture = () => {
    setShutter(true);
    setTimeout(() => setShutter(false), 200);
    setTimeout(() => setPhase('precheck'), 250);
    setTimeout(() => setPhase('warned'), 1400);
  };

  return (
    <ScreenShell T={T} pad={false} bg="#0a0a0a">
      {/* Top bar */}
      <div style={{
        position: 'absolute', top: 40, left: 0, right: 0, zIndex: 3,
        display: 'flex', justifyContent: 'space-between', padding: '10px 18px',
      }}>
        <button onClick={onCancel} style={{
          width: 40, height: 40, borderRadius: 99, background: '#0008',
          border: 'none', display: 'grid', placeItems: 'center', cursor: 'pointer',
        }}><IconClose size={22} stroke="#fff" /></button>
        <div style={{
          padding: '8px 12px', borderRadius: 99, background: '#0008',
          color: '#fff', fontSize: 12, fontWeight: 500, display: 'flex', alignItems: 'center', gap: 6,
        }}>
          <IconLock size={12} stroke="#fff" /> On-device only
        </div>
      </div>

      {/* Faux viewfinder */}
      <div style={{ flex: 1, position: 'relative', overflow: 'hidden',
        background: 'radial-gradient(circle at 50% 60%, #6a4a3a 0%, #2a1a14 70%)' }}>
        {/* simulated skin texture */}
        <div style={{
          position: 'absolute', inset: 0, opacity: 0.4,
          backgroundImage: 'radial-gradient(circle at 30% 40%, #9b6a52 0%, transparent 35%), radial-gradient(circle at 70% 60%, #7a4a3a 0%, transparent 40%)',
        }} />
        {/* mole/spot */}
        <div style={{
          position: 'absolute', left: '52%', top: '48%', transform: 'translate(-50%, -50%)',
          width: 38, height: 32, borderRadius: '50%',
          background: 'radial-gradient(circle, #2a1810 30%, #4a2c1e 70%)',
        }} />

        {/* framing reticle */}
        <div style={{
          position: 'absolute', inset: '30% 18% 30% 18%',
          border: '2px solid #ffffff66', borderRadius: 16,
        }}>
          {['top left','top right','bottom left','bottom right'].map((c, i) => {
            const top = c.includes('top') ? -2 : 'auto';
            const bottom = c.includes('bottom') ? -2 : 'auto';
            const left = c.includes('left') ? -2 : 'auto';
            const right = c.includes('right') ? -2 : 'auto';
            return (
              <div key={i} style={{
                position: 'absolute', top, bottom, left, right, width: 18, height: 18,
                borderTop: c.includes('top') ? '3px solid #fff' : 'none',
                borderBottom: c.includes('bottom') ? '3px solid #fff' : 'none',
                borderLeft: c.includes('left') ? '3px solid #fff' : 'none',
                borderRight: c.includes('right') ? '3px solid #fff' : 'none',
                borderRadius: 4,
              }} />
            );
          })}
        </div>

        {/* shutter flash */}
        {shutter && <div style={{ position: 'absolute', inset: 0, background: '#fff', opacity: 0.6 }} />}

        {/* precheck overlay */}
        {phase === 'precheck' && (
          <div style={{
            position: 'absolute', inset: 0, background: '#000a',
            display: 'grid', placeItems: 'center',
          }}>
            <div style={{ textAlign: 'center', color: '#fff' }}>
              <div style={{ width: 44, height: 44, margin: '0 auto', border: `3px solid ${T.accent}`,
                borderTopColor: 'transparent', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
              <div style={{ marginTop: 14, fontSize: 14 }}>Checking quality…</div>
            </div>
          </div>
        )}

        {/* warning sheet */}
        {phase === 'warned' && (
          <div style={{
            position: 'absolute', left: 16, right: 16, bottom: 110,
            padding: 16, borderRadius: 16, background: T.surface, border: `1px solid ${T.border}`,
            color: T.ink,
          }}>
            <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
              <div style={{
                width: 30, height: 30, borderRadius: 8, background: T.statusAmber + '22',
                display: 'grid', placeItems: 'center', flexShrink: 0,
              }}><IconAlert size={18} stroke={T.statusAmber} /></div>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 600, fontSize: 15 }}>A bit blurry — retake?</div>
                <div style={{ color: T.inkSoft, fontSize: 13, marginTop: 2 }}>
                  Hold steady about 6 inches away. Better light helps.
                </div>
              </div>
            </div>
            <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
              <Btn T={T} kind="secondary" onClick={() => setPhase('framing')}>Retake</Btn>
              <Btn T={T} onClick={onAccept}>Use anyway</Btn>
            </div>
          </div>
        )}
      </div>

      {/* Shutter row */}
      {phase === 'framing' && (
        <div style={{
          position: 'absolute', bottom: 32, left: 0, right: 0, zIndex: 3,
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 36,
        }}>
          <button style={{
            width: 44, height: 44, borderRadius: 12, background: '#0006',
            border: 'none', color: '#fff', cursor: 'pointer',
          }}>⚡</button>
          <button onClick={capture} style={{
            width: 78, height: 78, borderRadius: '50%', background: '#fff',
            border: '4px solid #fff8', cursor: 'pointer',
            boxShadow: 'inset 0 0 0 4px #000',
          }} />
          <button style={{
            width: 44, height: 44, borderRadius: 12, background: '#0006',
            border: 'none', color: '#fff', cursor: 'pointer', fontSize: 18,
          }}>↺</button>
        </div>
      )}

      <style>{`@keyframes spin { to { transform: rotate(360deg) } }`}</style>
    </ScreenShell>
  );
}

// ─── 6. TRIAGING (loading) ───────────────────────────────────

function TriagingScreen({ T, withImage, onDone }) {
  const steps = [
    "Reading what you said",
    withImage ? "Looking at the photo" : null,
    "Checking insurance options",
    "Drafting recommendation",
  ].filter(Boolean);
  const [i, setI] = useState(0);
  useEffect(() => {
    if (i >= steps.length) { const t = setTimeout(onDone, 500); return () => clearTimeout(t); }
    const t = setTimeout(() => setI(i + 1), 700);
    return () => clearTimeout(t);
  }, [i]);

  return (
    <ScreenShell T={T}>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column',
        alignItems: 'center', justifyContent: 'center', gap: 32, padding: 24 }}>
        <div style={{ position: 'relative', width: 72, height: 72 }}>
          <BrandMark size={72} color={T.accent} bg={T.accentSoft} />
          <div style={{
            position: 'absolute', inset: -10, border: `2px solid ${T.accent}`,
            borderTopColor: 'transparent', borderRadius: 22,
            animation: 'spin 1.2s linear infinite',
          }} />
        </div>
        <div style={{ textAlign: 'center' }}>
          <Display T={T} size={24}>One moment…</Display>
          <div style={{ color: T.inkSoft, marginTop: 6, fontSize: 14 }}>Running on your phone, not the cloud.</div>
        </div>
        <div style={{ width: '100%', maxWidth: 280, display: 'flex', flexDirection: 'column', gap: 10 }}>
          {steps.map((s, idx) => (
            <div key={s} style={{
              display: 'flex', alignItems: 'center', gap: 10, fontSize: 14,
              color: idx < i ? T.ink : T.inkMuted, opacity: idx <= i ? 1 : 0.5,
            }}>
              <div style={{ width: 16, height: 16, borderRadius: 99,
                background: idx < i ? T.accent : T.surfaceAlt,
                display: 'grid', placeItems: 'center' }}>
                {idx < i && <IconCheck size={11} stroke="#fff" sw={3} />}
              </div>
              {s}
            </div>
          ))}
        </div>
      </div>
    </ScreenShell>
  );
}

// ─── 7. RESULT ──────────────────────────────────────────────

const SEVERITY = {
  emergency:  { label: 'Emergency',         color: 'statusRed',    icon: IconAlert,   tag: 'Call 911 now' },
  urgent:     { label: 'Urgent care',       color: 'statusAmber',  icon: IconHospital,tag: 'Go in person today' },
  telehealth: { label: 'Telehealth',        color: 'statusBlue',   icon: IconPhone,   tag: 'Talk to a clinician' },
  selfcare:   { label: 'Self-care',         color: 'statusGreen',  icon: IconHome,    tag: 'Manage at home' },
};

function ResultScreen({ T, severity, onAction, onUpload, onRestart, scenario }) {
  const meta = SEVERITY[severity];
  const color = T[meta.color];
  const ActionIcon = meta.icon;

  const data = {
    emergency: {
      headline: 'Get to an emergency room now.',
      reasoning: "What you're describing — sudden, crushing chest pain radiating to the left arm — needs ER care immediately. This recommendation is based on standard emergency criteria, not a model guess.",
      flags: ['Chest pain > 15 min', 'Radiation to left arm', 'Sudden onset'],
      action: 'Call 911',
      actionIcon: IconPhone,
      confidence: '—',
      shortcircuit: true,
    },
    urgent: {
      headline: 'Worth a same-day in-person visit.',
      reasoning: "The mole has changed shape and color over a few weeks. A clinician should look at it within a day or two — likely benign, but worth checking. Your plan covers an in-network urgent dermatology visit.",
      flags: ['Asymmetric border', 'Color variation', 'Size growth'],
      action: 'Find urgent care',
      actionIcon: IconHospital,
      confidence: '0.78',
    },
    telehealth: {
      headline: "A video visit should sort this out.",
      reasoning: "Red, goopy eye in a child for under a day fits viral or bacterial conjunctivitis. A video visit can usually decide and prescribe drops if needed. Your plan: $15 copay with PediaCare.",
      flags: ['Redness', 'Discharge', 'No vision change'],
      action: 'Start video visit',
      actionIcon: IconPhone,
      confidence: '0.82',
    },
    selfcare: {
      headline: 'Rest, fluids, and watch how you feel.',
      reasoning: "A 3-day sore throat with mild ear discomfort, no fever, no trouble swallowing, no spreading rash — typically a viral cold. Try warm fluids and rest. Come back if symptoms worsen or last past 7 days.",
      flags: ['No fever', 'No trouble breathing', 'Mild discomfort'],
      action: 'See self-care tips',
      actionIcon: IconHeart,
      confidence: '0.71',
    },
  }[severity];

  return (
    <ScreenShell T={T} scroll>
      {/* Top header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 0' }}>
        <button onClick={onRestart} style={{ background: 'none', border: 'none', cursor: 'pointer',
          color: T.inkSoft, fontSize: 14, padding: 4 }}>← Start over</button>
        <PrivacyBadge T={T} compact />
      </div>

      {/* Severity badge */}
      <div style={{ marginTop: 16 }}>
        <div style={{
          display: 'inline-flex', alignItems: 'center', gap: 10,
          padding: '8px 14px 8px 10px', borderRadius: 999,
          background: color + '18',
          color: color, fontWeight: 600, fontSize: 14,
          border: `1px solid ${color}33`,
        }}>
          <span style={{ width: 8, height: 8, borderRadius: 99, background: color }} />
          {meta.label.toUpperCase()} · {meta.tag}
        </div>
      </div>

      <div style={{ marginTop: 14 }}>
        <Display T={T} size={28}>{data.headline}</Display>
      </div>

      {/* What Nora saw */}
      <div style={{
        marginTop: 22, padding: 18, borderRadius: 16,
        background: T.surface, border: `1px solid ${T.border}`,
      }}>
        <div style={{ display: 'flex', gap: 10, alignItems: 'center', marginBottom: 8 }}>
          <BrandMark size={26} color={T.accent} bg={T.accentSoft} />
          <div style={{ fontSize: 13, color: T.inkSoft, fontWeight: 600, whiteSpace: 'nowrap' }}>What I'm seeing</div>
        </div>
        <div style={{ fontSize: 16, color: T.ink, lineHeight: 1.5 }}>
          {data.reasoning}
        </div>
        <div style={{ marginTop: 14, display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {data.flags.map(f => (
            <div key={f} style={{
              padding: '5px 10px', borderRadius: 99, fontSize: 12,
              background: T.surfaceAlt, color: T.inkSoft, border: `1px solid ${T.border}`,
            }}>{f}</div>
          ))}
        </div>
        {!data.shortcircuit && (
          <div style={{ marginTop: 12, display: 'flex', justifyContent: 'space-between',
            fontSize: 11, color: T.inkMuted, fontFamily: T.fontMono }}>
            <span>MedGemma · on-device</span>
            <span>confidence {data.confidence}</span>
          </div>
        )}
        {data.shortcircuit && (
          <div style={{ marginTop: 12, display: 'flex', alignItems: 'center', gap: 6,
            fontSize: 11, color: T.statusRed, fontFamily: T.fontMono }}>
            <IconAlert size={12} stroke={T.statusRed} />
            <span>Safety rule fired — bypassed model</span>
          </div>
        )}
      </div>

      {/* Primary action */}
      <div style={{ marginTop: 18 }}>
        <button onClick={onAction} style={{
          width: '100%', padding: '18px 22px', borderRadius: 18,
          background: color, color: '#fff', border: 'none', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12,
          fontFamily: T.fontBody, boxShadow: `0 8px 24px -10px ${color}99`,
          textAlign: 'left',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 14, minWidth: 0, flex: 1 }}>
            <div style={{ width: 42, height: 42, borderRadius: 12, flexShrink: 0,
              background: '#ffffff22', display: 'grid', placeItems: 'center' }}>
              <data.actionIcon size={22} stroke="#fff" sw={2} />
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0, lineHeight: 1.2 }}>
              <div style={{ fontSize: 18, fontWeight: 600, lineHeight: 1.2 }}>{data.action}</div>
              <div style={{ fontSize: 12, opacity: 0.85, lineHeight: 1.3, marginTop: 2 }}>One tap</div>
            </div>
          </div>
          <IconChevron size={20} stroke="#fff" />
        </button>
      </div>

      {/* Optional escalate */}
      {severity !== 'selfcare' && severity !== 'emergency' && (
        <div style={{ marginTop: 12 }}>
          <Btn T={T} kind="secondary" onClick={onUpload} icon={<IconDoc size={18} stroke={T.ink} />}>
            Send my recent labs (de-identified)
          </Btn>
        </div>
      )}

      <div style={{ height: 12 }} />

      <div style={{ fontSize: 12, color: T.inkMuted, textAlign: 'center', marginTop: 8, lineHeight: 1.5 }}>
        Nora is a guide, not a diagnosis. Trust your gut — if something feels worse than this says, seek care.
      </div>
    </ScreenShell>
  );
}

// ─── 8. DE-ID DOC UPLOAD ────────────────────────────────────

function DeidUploadScreen({ T, onBack, onDone }) {
  const [phase, setPhase] = useState('preview'); // preview | extracting | scrubbing | sending | done

  useEffect(() => {
    if (phase === 'preview' || phase === 'done') return;
    const next = { extracting: 'scrubbing', scrubbing: 'sending', sending: 'done' }[phase];
    const t = setTimeout(() => setPhase(next), phase === 'sending' ? 1100 : 900);
    return () => clearTimeout(t);
  }, [phase]);

  const phases = [
    { key: 'extracting', label: 'Reading the document', mono: 'medgemma → JSON' },
    { key: 'scrubbing', label: 'Scrubbing personal info', mono: 'tanaos → [PATIENT_NAME_1]' },
    { key: 'sending', label: 'Sending to your provider', mono: 'POST · de-identified only' },
  ];
  const idx = phases.findIndex(p => p.key === phase);

  return (
    <ScreenShell T={T} scroll>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '8px 0' }}>
        <button onClick={onBack} style={{ background: 'none', border: 'none', cursor: 'pointer', color: T.ink, padding: 4 }}>
          <IconBack size={22} stroke={T.ink} />
        </button>
        <div style={{ fontWeight: 600, fontSize: 16 }}>Send to provider</div>
      </div>

      <div style={{ marginTop: 12 }}>
        <Display T={T} size={24}>Lab report · April 22</Display>
        <div style={{ color: T.inkSoft, fontSize: 14, marginTop: 6 }}>
          Anything personal gets replaced with placeholders before it leaves your phone.
        </div>
      </div>

      {/* Document preview */}
      <div style={{
        marginTop: 18, padding: 16, borderRadius: 16,
        background: T.surface, border: `1px solid ${T.border}`,
        fontFamily: T.fontMono, fontSize: 12, lineHeight: 1.7, color: T.ink,
      }}>
        <DocLine T={T} label="Patient" raw="Maria Hernandez" placeholder="[PATIENT_NAME_1]" scrubbed={phase !== 'preview'} />
        <DocLine T={T} label="DOB" raw="1979-03-14" placeholder="[DOB_1]" scrubbed={phase !== 'preview'} />
        <DocLine T={T} label="MRN" raw="A-2849-1077" placeholder="[MRN_1]" scrubbed={phase !== 'preview'} />
        <div style={{ height: 8 }} />
        <div style={{ color: T.inkSoft }}>Hemoglobin A1c · <span style={{ color: T.ink }}>6.7%</span></div>
        <div style={{ color: T.inkSoft }}>Fasting glucose · <span style={{ color: T.ink }}>132 mg/dL</span></div>
        <div style={{ color: T.inkSoft }}>LDL cholesterol · <span style={{ color: T.ink }}>148 mg/dL</span></div>
        <div style={{ height: 8 }} />
        <DocLine T={T} label="Provider" raw="Dr. James Chen" placeholder="[PROVIDER_1]" scrubbed={phase !== 'preview'} />
      </div>

      {/* Pipeline */}
      <div style={{ marginTop: 18, display: 'flex', flexDirection: 'column', gap: 10 }}>
        {phases.map((p, i) => {
          const state = idx > i || phase === 'done' ? 'done' : idx === i ? 'active' : 'pending';
          return (
            <div key={p.key} style={{
              display: 'flex', alignItems: 'center', gap: 12,
              padding: '10px 14px', borderRadius: 12,
              background: state === 'active' ? T.accentSoft : T.surface,
              border: `1px solid ${T.border}`,
              opacity: state === 'pending' ? 0.5 : 1,
            }}>
              <div style={{ width: 22, height: 22, borderRadius: 99,
                background: state === 'done' ? T.accent : 'transparent',
                border: state === 'done' ? 'none' : `2px solid ${T.inkMuted}`,
                display: 'grid', placeItems: 'center', flexShrink: 0,
              }}>
                {state === 'done' && <IconCheck size={13} stroke="#fff" sw={3} />}
                {state === 'active' && (
                  <div style={{ width: 8, height: 8, borderRadius: 99, background: T.accent, animation: 'blink 1s infinite' }} />
                )}
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 14, fontWeight: 500, color: T.ink }}>{p.label}</div>
                <div style={{ fontSize: 11, color: T.inkMuted, fontFamily: T.fontMono }}>{p.mono}</div>
              </div>
            </div>
          );
        })}
      </div>

      <div style={{ flex: 1 }} />

      {phase === 'preview' && (
        <div style={{ marginTop: 18 }}>
          <Btn T={T} onClick={() => setPhase('extracting')} big icon={<IconShield size={18} stroke="#fff" />}>
            Send de-identified
          </Btn>
        </div>
      )}
      {phase === 'done' && (
        <div style={{ marginTop: 18, display: 'flex', flexDirection: 'column', gap: 10 }}>
          <div style={{
            padding: 14, borderRadius: 12,
            background: T.statusGreen + '18', color: T.statusGreen,
            display: 'flex', alignItems: 'center', gap: 10, fontWeight: 500,
          }}>
            <IconCheck size={18} stroke={T.statusGreen} sw={3} />
            Sent to PediaCare. They'll reply within an hour.
          </div>
          <Btn T={T} onClick={onDone}>Done</Btn>
        </div>
      )}
    </ScreenShell>
  );
}

function DocLine({ T, label, raw, placeholder, scrubbed }) {
  return (
    <div style={{ display: 'flex', gap: 8 }}>
      <span style={{ color: T.inkSoft, minWidth: 70 }}>{label}</span>
      <span style={{
        color: scrubbed ? T.accent : T.ink,
        background: scrubbed ? T.accentSoft : 'transparent',
        padding: scrubbed ? '0 4px' : 0, borderRadius: 4,
        transition: 'all 250ms ease',
      }}>
        {scrubbed ? placeholder : raw}
      </span>
    </div>
  );
}

Object.assign(window, {
  SplashScreen, PermissionsScreen, IntakeScreen, CameraOfferScreen,
  CameraCaptureScreen, TriagingScreen, ResultScreen, DeidUploadScreen,
});

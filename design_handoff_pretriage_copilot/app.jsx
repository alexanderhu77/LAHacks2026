// app.jsx — root: theme + scenario + navigation + Tweaks panel.

const { useState, useEffect: useEff } = React;

const SCENARIOS = {
  selfcare: {
    label: 'Sore throat',
    transcript: "I've had a sore throat for three days and now my ear is hurting too.",
    severity: 'selfcare',
    showCamera: false,
  },
  telehealth: {
    label: 'Pink eye (child)',
    transcript: "My five-year-old's eye is red and goopy this morning.",
    severity: 'telehealth',
    showCamera: true,
  },
  urgent: {
    label: 'Changing mole',
    transcript: "I have a dark mole on my arm that's been growing and looks uneven.",
    severity: 'urgent',
    showCamera: true,
  },
  emergency: {
    label: 'Chest pain',
    transcript: "I've had crushing chest pain for twenty minutes radiating to my left arm.",
    severity: 'emergency',
    showCamera: false,
  },
};

const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
  "theme": "warm",
  "scenario": "selfcare",
  "showFrame": true
}/*EDITMODE-END*/;

function App() {
  const [tweaks, setTweak] = useTweaks(TWEAK_DEFAULTS);
  const T = window.THEMES[tweaks.theme] || window.THEMES.warm;
  const scenario = SCENARIOS[tweaks.scenario] || SCENARIOS.selfcare;

  const [screen, setScreen] = useState('splash');
  const [hasImage, setHasImage] = useState(false);

  // When scenario changes, reset to splash so the demo plays fresh
  useEff(() => { setScreen('splash'); setHasImage(false); }, [tweaks.scenario]);

  const goto = (name) => setScreen(name);
  const restart = () => { setScreen('splash'); setHasImage(false); };

  const renderScreen = () => {
    switch (screen) {
      case 'splash':
        return <SplashScreen T={T} onDone={() => goto('permissions')} />;
      case 'permissions':
        return <PermissionsScreen T={T} onAllow={() => goto('intake')} onSkip={() => goto('intake')} />;
      case 'intake':
        return <IntakeScreen T={T} presetTranscript={scenario.transcript}
          onSubmit={() => {
            if (scenario.severity === 'emergency') goto('triaging');
            else if (scenario.showCamera) goto('cameraOffer');
            else goto('triaging');
          }} />;
      case 'cameraOffer':
        return <CameraOfferScreen T={T}
          onCapture={() => goto('cameraCapture')}
          onSkip={() => goto('triaging')} />;
      case 'cameraCapture':
        return <CameraCaptureScreen T={T}
          onCancel={() => goto('cameraOffer')}
          onAccept={() => { setHasImage(true); goto('triaging'); }} />;
      case 'triaging':
        return <TriagingScreen T={T} withImage={hasImage} onDone={() => goto('result')} />;
      case 'result':
        return <ResultScreen T={T} severity={scenario.severity} scenario={scenario}
          onAction={() => {}} onUpload={() => goto('deid')} onRestart={restart} />;
      case 'deid':
        return <DeidUploadScreen T={T} onBack={() => goto('result')} onDone={restart} />;
      default: return null;
    }
  };

  // Step indicator (above frame)
  const STEPS = ['splash','permissions','intake','cameraOffer','cameraCapture','triaging','result','deid'];
  const stepLabels = {
    splash: 'Splash', permissions: 'Permissions', intake: 'Intake',
    cameraOffer: 'Camera offer', cameraCapture: 'Camera', triaging: 'Triaging',
    result: 'Result', deid: 'De-id upload',
  };

  // Status bar / nav uses theme-aware dark/light
  const dark = T.chrome === 'dark';

  return (
    <div style={{
      width: '100%', height: '100%', display: 'flex',
      alignItems: 'center', justifyContent: 'center', gap: 40, padding: 32,
      boxSizing: 'border-box',
    }}>
      {/* Left rail: project name + scenario chips */}
      <div style={{ width: 240, display: 'flex', flexDirection: 'column', gap: 24,
        fontFamily: 'Inter, system-ui', color: '#2a2a28' }}>
        <div>
          <div style={{ fontFamily: '"Fraunces", Georgia, serif', fontSize: 28, fontWeight: 500, lineHeight: 1.05, letterSpacing: '-0.01em' }}>
            Pre-triage<br/>co-pilot
          </div>
          <div style={{ fontSize: 13, color: '#6b675c', marginTop: 6 }}>
            Voice-first symptom intake, on-device.
          </div>
        </div>

        <div>
          <div style={{ fontSize: 11, fontWeight: 600, letterSpacing: 0.6, textTransform: 'uppercase', color: '#8e8a7d', marginBottom: 8 }}>Demo scenario</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            {Object.entries(SCENARIOS).map(([key, s]) => (
              <button key={key} onClick={() => setTweak('scenario', key)} style={{
                textAlign: 'left', padding: '10px 12px', borderRadius: 10,
                border: `1px solid ${tweaks.scenario === key ? '#5b7a63' : '#e3dac3'}`,
                background: tweaks.scenario === key ? '#dfe7d9' : '#fbf6ec',
                color: '#2a2a28', cursor: 'pointer', fontSize: 14, fontWeight: 500,
                fontFamily: 'inherit',
              }}>
                {s.label}
                <div style={{ fontSize: 11, color: '#8e8a7d', marginTop: 2, fontWeight: 400 }}>
                  → {s.severity}
                </div>
              </button>
            ))}
          </div>
        </div>

        <div>
          <div style={{ fontSize: 11, fontWeight: 600, letterSpacing: 0.6, textTransform: 'uppercase', color: '#8e8a7d', marginBottom: 8 }}>Flow step</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            {STEPS.map(s => (
              <button key={s} onClick={() => setScreen(s)} style={{
                textAlign: 'left', padding: '6px 10px', borderRadius: 8,
                border: 'none',
                background: screen === s ? '#efe7d4' : 'transparent',
                color: screen === s ? '#2a2a28' : '#6b675c',
                cursor: 'pointer', fontSize: 13, fontFamily: 'inherit',
              }}>
                {screen === s ? '● ' : '○ '}{stepLabels[s]}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Phone */}
      <div style={{ position: 'relative' }}>
        <AndroidDevice width={392} height={812} dark={dark}>
          <div style={{ position: 'relative', flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            {renderScreen()}
          </div>
        </AndroidDevice>
      </div>

      {/* Tweaks panel */}
      <TweaksPanel title="Tweaks">
        <TweakSection title="Color theme">
          <TweakRadio
            value={tweaks.theme}
            onChange={v => setTweak('theme', v)}
            options={[
              { value: 'warm', label: 'Warm' },
              { value: 'calm', label: 'Calm' },
              { value: 'bold', label: 'Bold' },
              { value: 'accessible', label: 'A11y' },
            ]}
          />
        </TweakSection>
        <TweakSection title="Scenario">
          <TweakSelect
            value={tweaks.scenario}
            onChange={v => setTweak('scenario', v)}
            options={Object.entries(SCENARIOS).map(([k, s]) => ({ value: k, label: s.label }))}
          />
        </TweakSection>
      </TweaksPanel>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);

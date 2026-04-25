// icons.jsx — line icons. Simple geometry only.

const Icon = ({ d, size = 24, stroke = 'currentColor', fill = 'none', sw = 1.75, children, vb = 24 }) => (
  <svg width={size} height={size} viewBox={`0 0 ${vb} ${vb}`} fill={fill} stroke={stroke}
       strokeWidth={sw} strokeLinecap="round" strokeLinejoin="round">
    {d ? <path d={d} /> : children}
  </svg>
);

const IconMic = (p) => (
  <Icon {...p}>
    <rect x="9" y="3" width="6" height="12" rx="3" />
    <path d="M5 11a7 7 0 0 0 14 0" />
    <path d="M12 18v3" />
  </Icon>
);

const IconKeyboard = (p) => (
  <Icon {...p}>
    <rect x="2.5" y="6" width="19" height="12" rx="2" />
    <path d="M6 10h.01M9 10h.01M12 10h.01M15 10h.01M18 10h.01" />
    <path d="M7 14h10" />
  </Icon>
);

const IconCamera = (p) => (
  <Icon {...p}>
    <path d="M3 8h3l1.5-2h9L18 8h3v11H3z" />
    <circle cx="12" cy="13" r="3.5" />
  </Icon>
);

const IconLock = (p) => (
  <Icon {...p}>
    <rect x="5" y="11" width="14" height="9" rx="2" />
    <path d="M8 11V8a4 4 0 0 1 8 0v3" />
  </Icon>
);

const IconClose = (p) => (
  <Icon {...p}>
    <path d="M6 6l12 12M18 6L6 18" />
  </Icon>
);

const IconBack = (p) => (
  <Icon {...p}>
    <path d="M15 6l-6 6 6 6" />
  </Icon>
);

const IconCheck = (p) => (
  <Icon {...p}>
    <path d="M5 12.5l4.5 4.5L19 7.5" />
  </Icon>
);

const IconAlert = (p) => (
  <Icon {...p}>
    <path d="M12 3l10 17H2z" />
    <path d="M12 10v5M12 17.5v.5" />
  </Icon>
);

const IconHeart = (p) => (
  <Icon {...p}>
    <path d="M12 20s-7-4.5-7-10a4 4 0 0 1 7-2.5A4 4 0 0 1 19 10c0 5.5-7 10-7 10z" />
  </Icon>
);

const IconHospital = (p) => (
  <Icon {...p}>
    <path d="M4 21V7l8-4 8 4v14" />
    <path d="M9 21v-5h6v5" />
    <path d="M12 9v4M10 11h4" />
  </Icon>
);

const IconPhone = (p) => (
  <Icon {...p}>
    <path d="M5 4h3l2 5-2 1a11 11 0 0 0 6 6l1-2 5 2v3a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2z" />
  </Icon>
);

const IconHome = (p) => (
  <Icon {...p}>
    <path d="M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z" />
  </Icon>
);

const IconDoc = (p) => (
  <Icon {...p}>
    <path d="M6 3h8l4 4v14H6z" />
    <path d="M14 3v4h4" />
    <path d="M9 13h6M9 17h6" />
  </Icon>
);

const IconSparkle = (p) => (
  <Icon {...p}>
    <path d="M12 3v6M12 15v6M3 12h6M15 12h6" />
  </Icon>
);

const IconChevron = (p) => (
  <Icon {...p}>
    <path d="M9 6l6 6-6 6" />
  </Icon>
);

const IconWaveform = (p) => (
  <Icon {...p}>
    <path d="M3 12h2M7 7v10M11 4v16M15 8v8M19 11v2" />
  </Icon>
);

const IconShield = (p) => (
  <Icon {...p}>
    <path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z" />
    <path d="M9 12l2 2 4-4" />
  </Icon>
);

// Branded mark — abstract pulse + cross
const BrandMark = ({ size = 56, color = '#5b7a63', bg = '#dfe7d9' }) => (
  <div style={{
    width: size, height: size, borderRadius: size * 0.28,
    background: bg, display: 'grid', placeItems: 'center',
    boxShadow: `inset 0 0 0 1px ${color}22`,
  }}>
    <svg width={size * 0.58} height={size * 0.58} viewBox="0 0 24 24" fill="none"
         stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M2 12h3l2-5 3 10 3-7 2 4 2-2h5" />
    </svg>
  </div>
);

Object.assign(window, {
  Icon, IconMic, IconKeyboard, IconCamera, IconLock, IconClose, IconBack,
  IconCheck, IconAlert, IconHeart, IconHospital, IconPhone, IconHome,
  IconDoc, IconSparkle, IconChevron, IconWaveform, IconShield, BrandMark,
});

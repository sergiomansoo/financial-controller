import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: { colors: { ledger: { bg: '#14161B', surface: '#1C1F26', panel: '#242832', border: '#2E323C', text: '#EDEAE3', muted: '#8B8F98' }, 'accent-income': '#2FA88A', 'accent-expense': '#A6435C', warning: '#D9A441', link: '#4C8BF5' }, fontFamily: { display: ['Fraunces'], ui: ['IBM Plex Sans'], mono: ['IBM Plex Mono'] } },
  },
  plugins: [],
} satisfies Config

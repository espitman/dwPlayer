/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        background: '#0B0F19',
        surface: '#131A2B',
        card: '#1B243B',
        accent: '#3B82F6',
        accentLight: '#60A5FA',
        accentGlow: 'rgba(59, 130, 246, 0.35)',
      },
    },
  },
  plugins: [],
}

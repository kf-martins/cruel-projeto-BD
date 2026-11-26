/** Tailwind lê as cores dos tokens CSS de src/index.css (tema Espresso/Latte). */
/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: 'var(--bg-primary)',
        secondary: 'var(--bg-secondary)',
        card: 'var(--bg-card)',
        'card-hover': 'var(--bg-card-hover)',
        ink: 'var(--text-primary)',
        'ink-soft': 'var(--text-secondary)',
        'ink-muted': 'var(--text-muted)',
        accent: 'var(--accent-primary)',
        'accent-hover': 'var(--accent-hover)',
        'accent-soft': 'var(--accent-soft)',
        'on-accent': 'var(--text-on-accent)',
        line: 'var(--border-subtle)',
        good: 'var(--status-good)',
        'good-soft': 'var(--status-good-soft)',
        warn: 'var(--status-warning)',
        'warn-soft': 'var(--status-warning-soft)',
        bad: 'var(--status-critical)',
        'bad-soft': 'var(--status-critical-soft)',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'Segoe UI', 'Roboto', 'sans-serif'],
        display: ['Fraunces', 'Georgia', 'serif'],
      },
      boxShadow: {
        card: '0 1px 2px rgb(0 0 0 / 0.06), 0 4px 16px -8px rgb(0 0 0 / 0.18)',
      },
    },
  },
  plugins: [],
};

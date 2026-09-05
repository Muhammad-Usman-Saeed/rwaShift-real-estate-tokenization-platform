import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        ink: {
          50: "#f4f6f9",
          100: "#e7ebf1",
          200: "#c9d3e0",
          300: "#a3b2c8",
          400: "#7488a8",
          500: "#54688a",
          600: "#3f5170",
          700: "#324259",
          800: "#1c2637", // primary text / brand
          900: "#12192a",
          950: "#0a0f1c",
        },
        // Recolored to match the RWA Shift logo mark (public/logo.png) — vivid blue + metallic
        // gold, replacing the previous muted navy/bronze scales. Same step count/shape as before,
        // just brighter/more saturated hues.
        brand: {
          50: "#eef4ff",
          100: "#dce8fe",
          200: "#b3cefd",
          300: "#82aefb",
          400: "#4f87f2",
          500: "#2f66df",
          600: "#234fb8",
          700: "#1d3f93", // primary brand
          800: "#1a3679",
          900: "#182c5f",
        },
        gold: {
          50: "#fdf8e7",
          100: "#faedc0",
          200: "#f3da7e",
          300: "#e9c247",
          400: "#d9a521",
          500: "#c08d13", // accent, used sparingly
          600: "#9c700f",
          700: "#7a570c",
        },
        success: {
          50: "#eef9f1",
          500: "#1f8a4c",
          600: "#186e3c",
          700: "#155c33",
        },
        warning: {
          50: "#fdf6ea",
          500: "#b5790a",
          600: "#946209",
          700: "#7a5108",
        },
        danger: {
          50: "#fbecec",
          500: "#b93a3a",
          600: "#992e2e",
          700: "#7d2626",
        },
        info: {
          50: "#eef4fb",
          500: "#2f6fac",
          600: "#255a8b",
        },
        surface: {
          DEFAULT: "#ffffff",
          subtle: "#f7f8fa",
          muted: "#eef0f3",
          border: "#e1e4ea",
        },
      },
      fontFamily: {
        sans: [
          "var(--font-inter)",
          "-apple-system",
          "BlinkMacSystemFont",
          "Segoe UI",
          "Helvetica Neue",
          "Arial",
          "sans-serif",
        ],
      },
      borderRadius: {
        sm: "4px",
        DEFAULT: "6px",
        md: "8px",
        lg: "12px",
        xl: "16px",
      },
      boxShadow: {
        card: "0 1px 2px rgba(18, 25, 42, 0.06), 0 1px 3px rgba(18, 25, 42, 0.08)",
        raised: "0 4px 12px rgba(18, 25, 42, 0.10)",
        focus: "0 0 0 3px rgba(47, 102, 223, 0.35)",
      },
      fontSize: {
        "2xs": ["0.6875rem", { lineHeight: "1rem" }],
      },
      keyframes: {
        marquee: {
          "0%": { transform: "translateX(0%)" },
          "100%": { transform: "translateX(-50%)" },
        },
      },
      animation: {
        // Track content is duplicated so the -50% end state seams back into the start seamlessly.
        // 120s is a deliberately slow, readable crawl — the ticker's job is to be skimmed at a
        // glance, not raced to catch before it scrolls off.
        marquee: "marquee 120s linear infinite",
      },
    },
  },
  plugins: [],
};

export default config;

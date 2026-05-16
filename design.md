# Design System Specification: Retro-Futuristic Terminal

## 1. Overview & Creative North Star

### The Creative North Star: "The Analog Sentinel"
This design system is a high-end exercise in **Atmospheric Functionalism**. It rejects the sterile, flat aesthetics of modern tech in favor of a tactile, retro-futuristic CRT experience. We are not just building an interface; we are designing a survival tool salvaged from an alternate timeline.

The system breaks the "standard template" look through **High-Contrast Phosphorus Emissivity**. By utilizing intentional asymmetry, heavy scanline textures, and a monochrome-first hierarchy, we create a UI that feels heavy, mechanical, and deeply intentional. The layout should feel like a diagnostic readout: dense with data but prioritized through glowing urgency.

---

## 2. Colors

The palette is rooted in the "Phosphorus Green" spectrum, designed to mimic the light emission of a cathode-ray tube against the infinite void of a wasteland night.

### Color Tokens
- **Background (`#131313`)**: The deep black of a powered-down monitor.
- **Primary / Glow (`#00FF41`)**: The heartbeat of the system. Used for critical data and interactive states.
- **Surface Tiers**:
    - `surface-container-lowest`: `#0e0e0e` (Used for "recessed" terminal wells)
    - `surface-container-low`: `#1c1b1b` (Standard sectioning)
    - `surface-container-highest`: `#353534` (Elevated mechanical plates)

### The "No-Line" Rule
Prohibit the use of 1px solid, static borders for sectioning. Structural boundaries must be defined by **Background Shifting**. Use `surface-container-low` for a main content block sitting on a `surface` background. If a container requires a border, it must be an "Emissive Stroke"—a 2px line using `primary` with a subtle outer glow (box-shadow) to simulate light bleed on a glass screen.

### Signature Textures
All surfaces must utilize a **Scanline Overlay**. Apply a repeating linear gradient or a fixed PNG pattern of horizontal lines (2px height, 10% opacity) across the entire viewport to ground the digital elements in physical hardware.

---

## 3. Typography

The typography is built on **Space Grotesk**, a typeface that balances technical precision with a slightly "off-grid" brutalist character.

| Role | Size | Intent |
| :--- | :--- | :--- |
| **Display-LG** | 3.5rem | System-level status or critical alerts. |
| **Headline-MD** | 1.75rem | Major terminal headers; always uppercase. |
| **Title-SM** | 1.0rem | Component labels and sub-headers. |
| **Body-MD** | 0.875rem | Default data readouts and descriptions. |
| **Label-SM** | 0.6875rem | Technical metadata and "micro-copy." |

**Editorial Note:** To achieve the "Terminal" look, increase letter-spacing by 0.05em for all Headline and Display styles. This mimics the character spacing of vintage hardware.

---

## 4. Elevation & Depth

In this system, depth is not "shadow," it is **Luminance and Recess**.

*   **Tonal Layering:** Instead of drop shadows, use `surface-container` tiers to create hierarchy. A "card" is not a raised element; it is a "window" cut into the dashboard. Use `surface-container-lowest` for input fields to make them feel "carved" into the hardware.
*   **The Layering Principle:** Stack `surface-container-high` on top of `surface-dim` to create a "module" effect.
*   **Ghost Borders:** For non-critical containment, use the `outline-variant` (`#3b4b37`) at 20% opacity. It should feel like a faint reflection on the glass, not a structural line.
*   **CRT Flicker:** Apply a subtle 0.05s infinite opacity animation (between 0.97 and 1.0) to the `primary` container to simulate power fluctuations.

---

## 5. Components

### Buttons
*   **Primary:** Solid `primary_container` (`#00FF41`) with `on_primary` text. No rounded corners (`rounded-none`). Add a 4px "glow" shadow of the same color.
*   **Secondary:** Ghost style. `primary` text and a 2px `primary` border. On hover, the background fills with 10% `primary` opacity.

### Input Fields
*   **Styling:** Background set to `surface-container-lowest`. 
*   **Focus State:** The border transitions from `outline` to a full `primary` glow. 
*   **Visual Cue:** Use a blinking underscore cursor (`_`) at the end of active text strings.

### Cards & Lists
*   **Divider Rule:** Strictly forbid horizontal lines. Use 24px or 32px of vertical whitespace to separate list items.
*   **Lists:** Leading icons should be thick-lined and monochromatic. Use `surface-container-low` as the background for alternating list items to create a "ribbon" effect without borders.

### Progress & Storage Bars
*   **Segmented Loading:** Do not use smooth transitions. Progress bars should move in "blocks" (discrete segments) using the `primary` color to emphasize the mechanical nature of the system.

---

## 6. Do's and Don'ts

### Do
*   **DO** use monochromatic icons. Every icon must be the same `primary` green or a dimmed variant.
*   **DO** use asymmetry. Place a small technical label (Label-SM) in the top-right of a container to offset a title in the top-left.
*   **DO** treat the edges of the screen as the bezel of a monitor. Keep content padded significantly (`32px+`) from the edge.

### Don't
*   **DON'T** use multi-color icons. This breaks the phosphorus immersion.
*   **DON'T** use standard "Material Design" rounded corners. Stick to `DEFAULT (0.25rem)` or `none` for a rigid, industrial feel.
*   **DON'T** use smooth, blurred shadows. If you need a shadow, it should be a hard-edged "offset" or a diffuse glow of the primary color.

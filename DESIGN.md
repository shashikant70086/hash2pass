# hash2pass Design System

## Design Philosophy

The interface of **hash2pass** is designed around a single principle:

> **Technology should feel tangible.**

Instead of flat UI or traditional neumorphism, the application adopts a **Liquid Glass** design language inspired by modern operating systems while maintaining the personality of an engineering dashboard.

The experience should feel like interacting with layers of polished glass floating above a digital blueprint.

---

# Core Design Principles

## 1. Liquid Glass

Every interactive component should appear as a physical sheet of translucent glass.

Characteristics include:

* Frosted transparency
* Dynamic reflections
* Specular highlights
* Soft refraction
* Internal light scattering
* Rounded optical edges
* Floating appearance
* Subtle depth

The goal is to create believable digital materials rather than decorative effects.

---

## 2. Motion Before Color

Motion communicates interaction.

Animations should be:

* smooth
* responsive
* subtle
* physically believable

Avoid:

* sudden scaling
* flashy transitions
* excessive bouncing

Preferred animation timing:

* 120–250ms for hover
* 250–400ms for component transitions
* spring easing whenever possible

---

## 3. Depth Hierarchy

Every component exists on a visual layer.

Layer order:

```
Background Grid

↓

Glass Tiles

↓

Cards

↓

Navigation

↓

Dialogs

↓

Notifications
```

Higher layers receive:

* stronger blur
* brighter reflections
* softer shadows

---

# Color Palette

## Background

```
#FAFAFA
```

## Sand

```
#EAE7E0
```

## Primary

```
#D94532
```

## Navy

```
#2C3A47
```

## Text

```
#1A1D24
```

Accent colors should only appear during interaction.

---

# Glass Material

Every glass component should contain multiple optical layers.

### Layer 1

Base transparency

```
rgba(255,255,255,0.12)
```

---

### Layer 2

Backdrop blur

```
blur(20px–30px)
```

---

### Layer 3

Specular reflection

Moving radial highlight that follows cursor position.

---

### Layer 4

Fresnel edge

Thin white border

```
rgba(255,255,255,0.35)
```

---

### Layer 5

Inner glow

Very soft white inset shadow.

---

### Layer 6

Outer shadow

Large soft shadow for floating depth.

---

# Interactive Grid

The background grid is a living surface.

Each tile reacts independently.

Behavior:

* cursor proximity
* touch interaction
* click activation
* moving reflections
* liquid highlights

Tiles should never feel static.

Neighboring tiles should softly react to nearby interactions.

---

# Navigation

Navigation is a floating glass capsule.

Characteristics:

* translucent
* blurred
* sticky
* reflective
* lightweight

The navigation should never look like a solid rectangle.

---

# Buttons

Buttons are miniature glass objects.

Hover:

* brighter reflection
* slight elevation
* stronger glow

Pressed:

* slight compression
* darker reflection
* reduced shadow

---

# Cards

Cards float above the grid.

Every card should include:

* glass background
* subtle border
* backdrop blur
* soft inner light
* floating shadow

Cards should feel suspended instead of attached.

---

# Lighting Model

Light originates from the user's cursor.

As the pointer moves:

* reflections move
* highlights shift
* glass edges brighten
* neighboring surfaces react

No fixed highlights.

Everything should feel physically illuminated.

---

# Typography

Font:

```
JetBrains Mono
```

Reasons:

* engineering aesthetic
* terminal inspiration
* high readability
* technical identity

Headings:

* uppercase
* tight tracking
* bold

Body text:

* relaxed spacing
* high contrast
* medium weight

---

# Motion Guidelines

Preferred easing:

```
ease-out
```

or

```
cubic-bezier(.22,.61,.36,1)
```

Never use harsh linear animations.

Animations should feel fluid, like moving through water.

---

# Accessibility

Despite the glass appearance:

* maintain WCAG AA contrast
* preserve text readability
* respect prefers-reduced-motion
* provide visible keyboard focus
* ensure touch-friendly controls

Beauty should never reduce usability.

---

# Performance

Visual richness should never compromise responsiveness.

Guidelines:

* animate transform and opacity only
* avoid layout recalculations
* batch DOM updates
* use requestAnimationFrame
* minimize expensive backdrop filters
* use CSS variables for dynamic lighting

Target:

* 60 FPS minimum
* 120 FPS where supported

---

# Future Enhancements

Planned visual improvements include:

* true screen-space refraction
* animated caustic lighting
* dynamic liquid distortion
* environment reflections
* cursor-driven Fresnel effects
* adaptive glass tinting
* GPU-accelerated shader backgrounds
* WebGL/WebGPU lighting
* volumetric glow
* depth-aware parallax

---

# Design Goal

The interface should not look like a typical dashboard.

It should resemble a piece of precision-engineered hardware brought to life—clean, tactile, responsive, and immersive.

Every interaction should reinforce the illusion that the user is manipulating real sheets of polished glass floating above a technical blueprint.

# M3 Forms

JSON Schema-driven form editor with multi-level meta-architecture.

## What This Is

A ClojureScript/re-frame application that generates interactive form UIs from JSON Schemas. It operates at four meta-levels:

- **M3** — The JSON meta-schema (schema that validates schemas). Self-referential. Editable in the developer pane.
- **M2** — A JSON Schema defining a data model (e.g., divorce financial disclosure, bond final terms). Editable in both developer and customer panes.
- **M1** — Instance data conforming to the M2 schema. This is what end-users fill in.
- **M0** — A Handlebars template that renders M1 data into a final document (e.g., a PDF-ready legal form).

The customer-facing pane uses Material UI (MUI) components. The developer pane uses raw HTML5 with colored backgrounds for quick iteration.

## Architecture

### Rendering Pipeline

`render-1` / `render-2` is a multimethod dispatching on `[ui-mode type format]`:
- `::html5` — colored HTML5 inputs (developer pane)
- `::mui` — Material UI components (customer pane)
- `(derive ::mui ::html5)` means MUI falls back to HTML5 for unimplemented types

`render-1` is the L2 (compile-time) call: `(render-1 c2 p2 k2 m2)` returns an L1 function `(fn [c1 p1 k1 m1] hiccup)`.

### Workflow System

Products have workflows defining states with views and transitions:
- **States**: each has a `$id`, a list of `views` (paths into the schema), and `transitions` (next/back buttons)
- **Views**: rendered as tabs when multiple, or as a single form when `["everything"]`
- **Stepper**: MUI stepper showing workflow progress (only when >1 state)

### Re-frame State

Key db keys: `:m3`, `:m2`, `:m1`, `:m0`, `:products`, `:product-id`, `:workflow`, `:state-id`, `:active-tab`, `:expanded`

Key events: `:select-product`, `:transition`, `:set-active-tab`, `:assoc-in`, `:update-in`, `:rename-in`, `:delete-in`, `:move`, `:expand`, `:collapse`

### Products

Defined in `core.cljs` as an `array-map`:
- **Final Terms** — bond issuance (has M0 Handlebars template for document generation)
- **Divorce** — financial disclosure with multi-state workflow
- **Demo** — simple example

## Key Files

- `src/cljs/m3_forms/core.cljs` — App entry point, re-frame events/subs, home-page component
- `src/cljs/m3_forms/mui.cljs` — MUI renderers, app-bar, view-forms, workflow stepper/tabs
- `src/cljs/m3_forms/render.cljs` — Render multimethod, HTML5 renderers, `render-1`/`render-2` dispatch
- `src/cljc/m3_forms/schema.cljc` — Schema operations (expand-$ref, check-schema, make-m3)
- `src/cljc/m3_forms/json.cljc` — JSON manipulation (absent/present?, json-insert-in, json-remove-in)
- `src/cljc/m3_forms/migrate.cljc` — Schema migration (rename/delete fields)
- `src/cljc/m3_forms/page.cljc` — Page derivation from workflow state
- `src/cljc/m3_forms/bind.cljc` — Binding system for schema/data extraction
- `src/cljc/m3_forms/divorce.cljc` — Divorce product (m2 schema, m1 data, workflow)
- `src/cljc/m3_forms/final_terms.cljc` — Final Terms product (m2, m1, m0 template, workflow)
- `src/cljc/m3_forms/demo.cljc` — Demo product (m2, m1)

## Development

```bash
# Start dev server (port 8280)
npm run dev
# or: npx shadow-cljs watch app

# One-off compile
npx shadow-cljs compile app

# Release build
npm run release
```

The dev server at http://localhost:8280 hot-reloads on save.

## Known Issues / Areas for Improvement

### High Priority
- **No tests** — Need schema validation tests, render pipeline tests, workflow state tests.

### Medium Priority
- **Developer pane quality** — 8-column table layout is crude, needs proper styling and collapsible sections.
- **Form validation UX** — No required field indicators, no inline error messages, no validation summary.

### Lower Priority
- **Drag-and-drop reordering** — Removed during migration. Array items can be added/deleted but not reordered.
- **M0 document generation** — Only works for Final Terms. Divorce and Demo have no M0 templates.
- **squidgy expand/collapse** — Only for objects and oneOf; arrays and deep types could benefit.
- **MUI theming** — No custom theme. Components use default MUI styling.

## Vision & Roadmap

The goal is a **multi-platform schema-driven form engine**:
- **Web** — MUI customer pane (current, working)
- **Mobile** — Capacitor/WebView wrapping the web pane, or React Native renderers
- **Developer** — HTML5 pane for schema iteration (needs polish)

The multimethod dispatch on `[ui-mode type format]` makes adding new rendering backends straightforward: `(derive ::new-backend ::html5)` + implement methods.

Future: multi-party workflows, broader applicability (legal, finance, HR, blockchain).

### Critical Architecture Note: Memoization Trap
`render = (memoize render-2)`. If a renderer calls `render-1` with arguments matching its own dispatch key, it recurses infinitely because the memoized first call hasn't returned yet. Always ensure inner `render-1` calls use shadowed/different arguments (e.g., item-level schema, not the parent array schema).

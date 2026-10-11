# Good4 Design System

## Product Identity
- **Project name:** Good4
- **Product type:** Multi-role commerce, reservations, and campaign management app
- **Platform stance:** Mobile-first Compose Multiplatform with theme-aware light and dark UI
- **Design intent:** Clean, practical, trustworthy, and data-rich without feeling heavy

## What The App Feels Like
- The interface is structured and card-driven.
- The visual language is calm, readable, and operational rather than decorative.
- White surfaces sit on a soft neutral canvas.
- Green is the main accent for success, selection, and primary attention.
- Yellow and warm orange are used sparingly for emphasis, not decoration.
- Error states are explicit and restrained.

## Visual Principles
- Use HeroUI visual language for new designs, adapted to Good4 semantic colors and native Compose interactions.
- Never use the default Material appearance for new cards, dialogs, buttons, or controls. Material rendering primitives may be used with explicit custom styling.
- Prioritize clear hierarchy over ornament.
- Keep spacing consistent and intentional.
- Prefer compact information blocks over large empty hero areas.
- Use strong labels, small supporting text, and bold numeric emphasis where needed.
- Keep interactions obvious: selected state, disabled state, loading state, and error state should be easy to read at a glance.

## Color System

### Semantic Tokens From Code
- **TextPrimary:** `#1D1D1B` for main body text and headings.
- **TextSecondary:** `#6E6E6B` for supporting text, metadata, and de-emphasized labels.
- **SurfaceDefault:** `#FFFFFF` for cards, sheets, and raised content.
- **SurfaceMuted:** `#F2F1ED` for soft blocks, previews, and subtle containers.
- **BorderMuted:** `#9D9A90` at low opacity for dividers and light borders.
- **PrimaryGreen:** `#008556` for brand-confirming success and strong actions.
- **DeepGreen:** `#A7D80A` for active state, selected navigation, score-like highlights, and positive status.
- **PistachioGreen:** `#E6F4C2` for soft success backgrounds and gentle emphasis.
- **AccentYellow:** `#FFE600` for high-visibility accent moments.
- **AccentWarm:** `#F0A870` for warm secondary emphasis.
- **ErrorRed:** `#D6483B` for destructive actions, errors, and cancellation.
- **WarningBackground:** `#FFF1DC` for caution surfaces.
- **WarningText:** `#A2552A` for warning text.

### Usage Rules
- Use `TextPrimary` for almost all primary copy.
- Use `TextSecondary` for metadata, helper text, and secondary labels.
- Use `SurfaceDefault` for the majority of cards and panels.
- Use `SurfaceMuted` for preview blocks, neutral boxes, and non-primary containers.
- Use `DeepGreen` for selected navigation items and positive chip states.
- Use `PrimaryGreen` when the screen needs a stronger brand-positive signal.
- Use `ErrorRed` only for destructive or invalid states.
- Resolve colors from the current Good4 theme; preserve contrast in both light and dark mode.

## Typography
- The project uses the native sans-serif typeface and shared typography; this does not prescribe Material component appearance.
- There is no custom display font or decorative type system.
- Headlines should be bold and concise.
- Section titles are usually medium-to-semi-bold.
- Supporting labels are small and muted.
- Numeric dashboard values should be visually dominant, often 24sp or larger.

### Observed Type Rhythm
- Large counters and hero metrics can reach 64sp in dashboard contexts.
- Section headings usually sit around 16sp to 18sp.
- Supporting labels commonly use 11sp to 14sp.
- Primary CTA labels are typically 14sp to 16sp with semi-bold weight.

## Shape And Elevation
- The default corner language is soft rounded rectangles.
- `12dp` radius is the most common form for cards, inputs, buttons, and sheets.
- `8dp` radius is used for inner chips, image placeholders, and small status surfaces.
- `16dp` radius appears in some larger informational blocks and badges.
- Card elevation is subtle:
- `1dp` to `2dp` for most cards.
- `4dp` for prominent transient UI like error snackbars.
- Borders are thin and muted, usually around `1dp`.

## Layout Language
- Screens prefer vertical stacking with predictable spacing.
- `LazyColumn` is the default for content-heavy pages.
- Keyed list items are used for performance and stability.
- Cards are padded tightly but not cramped.
- Empty states are centered and minimal.
- Many flows use bottom sheets instead of separate detail screens.
- Role-specific home screens often wrap content in a nested scaffold with a bottom navigation bar.

## Core Screen Patterns

### Auth
- Login and registration screens use a light background, centered content, and rounded outlined fields.
- Inputs are large enough for touch, usually `56dp` high or close to it.
- Focused outlines use `TextPrimary`.
- The app tagline is present on the login experience and sets a practical, mission-led tone.

### Home And Navigation
- **Student:** bottom navigation with product list, reservations, and profile.
- **Business:** bottom navigation with dashboard, code verification, products, and profile.
- **Supporter:** bottom navigation with home, cart or order summary, and profile.
- **Admin:** hybrid navigation with drawer plus top-level sections such as dashboard, products, campaigns, and profile.
- Selected nav icons and text use `DeepGreen`.
- Unselected nav items use `TextSecondary`.
- Navigation indicators stay transparent and unobtrusive.

### Dashboard
- Dashboards use a compact analytics layout with stat cards at the top.
- Stat cards pair a colored icon tile with a bold number and a muted label.
- Recent items are shown in dense cards or lists.
- Empty states are explicit and brief.

### Products
- Product cards are rounded, white, and lightly elevated.
- Product imagery uses clipped preview boxes.
- Price and stock information are visually separated.
- In-stock and out-of-stock states use green and red chip treatments.
- Admin product flows add business selection, while business flows focus on direct product management.

### Profile
- Profile screens use a shared scaffold, info cards, and a clear logout action.
- Logout is intentionally destructive-looking:
- `ErrorRed` outline or fill
- transparent or very light background in some cases
- Supporter and admin profiles also surface summary stats and role-specific controls.

### Reservations And Order Codes
- Reservation and code-status cards are information-dense and status-driven.
- Status badges are small chips with semantic color.
- Cancel actions are compact and clearly destructive.

### Campaign And Media
- Image preview blocks use muted neutral backgrounds and clipped corners.
- Campaign screens are card-based and often use a 12dp visual rhythm.

## Shared Component Language
- `Good4Scaffold` for full-screen pages.
- `Good4NestedScaffold` for role home screens with nested navigation.
- `ProfileScreenScaffold` for profile pages.
- `StatCard` for dashboard summaries.
- `ProductListCard` for product rows/cards.
- `ProductFormFields` for add/edit flows.
- `ImagePreviewBox` for media previews.
- `ReservationCard` for reservation and code workflows.
- `ErrorSnackbar` for transient failure states.

## Component Rules
- Forms should be consistent across add and edit flows.
- Reuse the same input styling and button rhythm across auth, product, and profile screens.
- Keep button radii consistent at `12dp`.
- Keep chip and mini-status radii slightly tighter than cards.
- Use borders and subtle surface tints before adding heavier shadows.
- Prefer shared components over per-screen variants when the pattern repeats.

## Interaction And Feedback
- Loading states are minimal and usually use circular progress indicators.
- Error handling is visible and direct, either inline or via snackbar.
- Validation feedback should appear close to the field that failed.
- Disabled primary actions should visibly soften, not disappear.
- Selected states should change both icon and text weight/color when appropriate.

## Copy And Localization
- UI strings are externalized in `composeResources/values/strings.xml`.
- Hardcoded UI text should be avoided.
- Copy tone is practical, short, and user-directed.
- The product language is primarily Turkish in the app UI.

## HeroUI Design Guidance
When designing new screens for Good4:
- Use the current theme's canvas and card surface tokens.
- Make green the primary active and success color.
- Keep layouts compact, structured, and operational.
- Favor softly rounded cards, subtle borders, and low-elevation surfaces; transient consent cards use 28dp corners with 14dp actions.
- Design for role-based navigation and multi-role content density.
- Treat the app as a commerce and services system, not a marketing site.

## Suggested Screen Families
- Auth
- Dashboard
- Products
- Profile
- Reservations
- Campaigns

## Do
- Use semantic color names and roles.
- Keep copy compact.
- Use strong hierarchy and predictable spacing.
- Reuse the shared components.
- Keep both themes readable and consistent.

## Do Not
- Do not hardcode a light-only surface into a theme-aware component.
- Do not use decorative gradients or heavy glass effects.
- Do not replace shared components with ad hoc layouts.
- Do not overuse accent colors.
- Do not hardcode strings in the UI.

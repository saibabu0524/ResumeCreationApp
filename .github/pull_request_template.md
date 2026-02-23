## Description

<!-- Briefly describe the changes in this PR. What problem does it solve? -->

## Type of Change

- [ ] 🐛 Bug fix (non-breaking change that fixes an issue)
- [ ] ✨ New feature (non-breaking change that adds functionality)
- [ ] 💥 Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] 📝 Documentation update
- [ ] 🔧 Refactor (no functional change)
- [ ] ✅ Test (adding or updating tests)
- [ ] 🔨 Build/CI (changes to build system or CI)

## Related Issue

<!-- Link to related issue if applicable: Closes #123 -->

## Screenshots (if UI change)

| Light Mode | Dark Mode |
|:----------:|:---------:|
| <!-- Add screenshot --> | <!-- Add screenshot --> |

---

## Pre-Flight Checklist

### Architecture
- [ ] Module boundaries respected — no cross-layer violations
- [ ] Business logic lives in UseCases (not ViewModel or Repository)
- [ ] Domain models have zero Android imports
- [ ] Feature modules do NOT depend on other features

### Code Quality
- [ ] `./gradlew ktlintFormat` — code is properly formatted
- [ ] `./gradlew detekt` — no quality issues
- [ ] No `!!` (force unwrap) in production code
- [ ] No hardcoded strings — all in `strings.xml`
- [ ] No hardcoded colors or dimensions — all in theme
- [ ] No `LiveData` introduced — using `StateFlow` / `Flow`
- [ ] No `Dispatchers.IO` hardcoded — injected via `@IoDispatcher`
- [ ] `collectAsStateWithLifecycle()` used (not `collectAsState()`)

### Tests
- [ ] New feature has corresponding unit tests
- [ ] All existing tests pass: `./gradlew test`
- [ ] Flows tested with Turbine (exact values, exact order)
- [ ] Fakes used over mocks for repository dependencies

### Compose UI
- [ ] New composables have Preview functions (light + dark)
- [ ] Touch targets ≥ 48.dp for all interactive elements
- [ ] `contentDescription` on all `Image` composables
- [ ] Composables are stateless — state hoisted to ViewModel
- [ ] `Modifier` is the last parameter with default `Modifier`

### Security
- [ ] No API keys or secrets in code
- [ ] ProGuard rules updated if new serializable classes added
- [ ] Dark mode tested and working

### Build
- [ ] `./gradlew assembleDebug` builds successfully
- [ ] No new warnings introduced

---

## Testing Instructions

<!-- How should reviewers test this change? -->

1. 
2. 
3. 

## Additional Notes

<!-- Any other context or notes for reviewers -->

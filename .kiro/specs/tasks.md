
- [ ] 11. AI Tool Integration for Brand Features - Extend AI tools.json with brand-specific tools for Needle3
  - [ ] Create apply_brand_kit tool
  - [ ] Create add_brand_logo tool with positioning
  - [ ] Create add_brand_footer tool
  - [ ] Create magic_resize tool
  - [ ] Create apply_template tool
  - [ ] Create suggest_brand_colors tool
  - [ ] Update model-manifest.json if needed
  - [ ] Test all tools with Needle3

- [ ] 12. Integration Testing - Create comprehensive integration tests for the branded content system
  - [ ] Create BrandKitIntegrationTest
  - [ ] Create ImageEditingIntegrationTest
  - [ ] Create TemplateIntegrationTest
  - [ ] Create MagicResizeIntegrationTest
  - [ ] Create EndToEndBrandingTest
  - [ ] Add Robolectric tests for UI components
  - [ ] Add instrumented tests for export workflow

- [ ] 13. Documentation and Examples - Update documentation with new features and create usage examples
  - [ ] Update AGENTS.md with new architecture
  - [ ] Update README.md with feature list
  - [ ] Create docs/brand-kit.md user guide
  - [ ] Create docs/image-editing.md user guide
  - [ ] Create docs/templates.md user guide
  - [ ] Create docs/magic-resize.md user guide
  - [ ] Update CHANGELOG.md

## Task Dependency Graph

- Task 1 (Brand Kit Data Models) has no dependencies
- Task 2 (Brand Kit UI Components) depends on Task 1
- Task 3 (Brand Kit Integration) depends on Task 1, Task 2
- Task 4 (Image Editing Data Models) has no dependencies
- Task 5 (Image Editing Tools for AI) depends on Task 4
- Task 6 (Image Editing UI) depends on Task 4, Task 5
- Task 7 (Branded Template System) depends on Task 1, Task 2
- Task 8 (Template Gallery UI) depends on Task 7
- Task 9 (Magic Resize Core) depends on Task 4, Task 7
- Task 10 (Magic Resize UI) depends on Task 9
- Task 11 (AI Tool Integration) depends on Task 3, Task 5, Task 9
- Task 12 (Integration Testing) depends on Task 1-11
- Task 13 (Documentation) depends on Task 1-12

## Notes

- All tasks follow the existing bapXcut architecture using Kotlin, ViewModels, and XML layouts
- Brand Kit uses SharedPreferences/JSON for local-first persistence
- Image editing tools extend the existing Needle3 AI tool system
- Magic Resize requires ML Kit for face detection (optional dependency)
- Template system supports multiple aspect ratios for social media platforms

## Acceptance Criteria Summary

- All data models serializable and immutable
- UI follows Material Design 3 with bapXcut brand colors (#000000, #6463D7, #875CE9, #545A67)
- AI tools have clear JSON schemas for Needle3
- Tests pass with >70% coverage for new code
- Documentation updated for all new features

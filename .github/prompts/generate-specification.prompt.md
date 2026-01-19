---
name: generate-specification
description: Generate a new feature specification in the memory bank
argument-hint: Provide a feature name and brief goal
agent: specification
---

# Generate Feature Specification

Feature name: ${input:featureName:Short, descriptive name}
Short description: ${input:summary:What problem does this solve?}
Constraints/notes: ${input:constraints:Key constraints, dependencies, or deadlines}

Please:
- Read the memory bank to align with requirements, ADRs, workflows, and domain knowledge.
- Ask clarifying questions before drafting the spec.
- Create .memory_bank/Features/<slug>/<slug>.spec.md using the template.
- Update .memory_bank/Requirements.md Feature Index and any relevant sections.
- If architectural decisions are needed, add or update an ADR in .memory_bank/ADRs.md.

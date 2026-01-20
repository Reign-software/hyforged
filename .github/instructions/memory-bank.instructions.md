---
name: Memory Bank Standards
description: Standards and templates for .memory_bank documentation
applyTo: ".memory_bank/**"
---

# Memory Bank Standards

## Purpose
The memory bank is a high-level knowledge base for requirements, architecture decisions, workflows, and feature summaries. It must remain concise and non-implementation-specific.

## General Rules
- Keep entries high-level; avoid code snippets and low-level design detail.
- Use the standardized templates in the memory bank.
- Prefer Mermaid diagrams for workflows and system flows when helpful.
- Use kebab-case for feature slugs and folder names.
- Update relevant indices and change logs when adding new entries.

## Required Files and Templates
- Requirements: .memory_bank/Requirements/_template_Requirements.md
- ADRs: .memory_bank/ADRs.md
- Domain Knowledge: .memory_bank/Domain-Knowledge.md
- Workflow template: .memory_bank/Workflows/_template.md
- Feature templates: .memory_bank/Features/_template/feature.spec.md and .memory_bank/Features/_template/feature.plan.md
- Review template: .memory_bank/Features/_template/reviews/_template.review.md

## Status Tracking
- Use checkboxes to track step completion in plans and workflows.
- Update phase status only when exit criteria are met.

## Review Severity
- Critical and Major findings are blocking.
- Minor findings are non-blocking unless explicitly required by stakeholders.

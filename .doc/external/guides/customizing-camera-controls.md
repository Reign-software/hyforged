# Customizing Camera Controls (Summary)

Source: https://hytalemodding.dev/en/docs/guides/plugin/customizing-camera-controls

## Purpose
Control the player camera using server packets.

## Key concepts
- Use `SetServerCamera` with `ServerCameraSettings`.
- Customize distance, lerp speeds, rotation, input mode, and cursor display.
- `ClientCameraView.Custom` enables custom camera settings.
- `PositionDistanceOffsetType.DistanceOffsetRaycast` helps avoid wall clipping.

## Tips
- Align movement with camera yaw using `MovementForceRotationType.Custom`.
- Lock an axis with `movementMultiplier` for 2D side‑scrolling.
- Use radians for rotation and convert degrees when needed.

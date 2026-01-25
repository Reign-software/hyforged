package reign.software.hyforged.passive.effect;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import reign.software.hyforged.passive.component.PassiveTreeComponent;
import reign.software.hyforged.passive.model.PassiveNode;
import reign.software.hyforged.passive.model.PassiveNodeEffect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.logging.Logger;

/**
 * Effect handler for mastery-choice type passive node effects.
 * <p>
 * Mastery nodes present the player with a choice when allocated. The chosen
 * option is stored in the passive tree component and its sub-effects are applied.
 * <p>
 * JSON effect format:
 * <pre>
 * {
 *   "Type": "mastery-choice",
 *   "Choices": [
 *     {
 *       "Id": "option1",
 *       "Name": "Fire Mastery",
 *       "Description": "Increases fire damage",
 *       "Effects": [
 *         { "Type": "stat-modifier", "Stat": "hyforged:fire_damage", "Value": 500, "StackType": "INCREASED" }
 *       ]
 *     },
 *     {
 *       "Id": "option2",
 *       "Name": "Ice Mastery",
 *       "Description": "Increases cold damage",
 *       "Effects": [
 *         { "Type": "stat-modifier", "Stat": "hyforged:cold_damage", "Value": 500, "StackType": "INCREASED" }
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 * <p>
 * When a mastery node is allocated:
 * 1. If no choice is stored, the UI prompts the player to choose
 * 2. Once a choice is made, the selected option's effects are applied
 * 3. On deallocation, the selected option's effects are removed
 */
public final class MasteryChoiceEffectHandler implements PassiveEffectHandler {

    private static final Logger LOGGER = Logger.getLogger(MasteryChoiceEffectHandler.class.getName());

    /**
     * Effect type identifier.
     */
    public static final String EFFECT_TYPE = "mastery-choice";

    private final ComponentType<EntityStore, PassiveTreeComponent> passiveTreeComponentType;

    /**
     * Create a new handler.
     *
     * @param passiveTreeComponentType The component type for PassiveTreeComponent
     */
    public MasteryChoiceEffectHandler(
            @Nonnull ComponentType<EntityStore, PassiveTreeComponent> passiveTreeComponentType
    ) {
        this.passiveTreeComponentType = passiveTreeComponentType;
    }

    @Override
    public void apply(@Nonnull Ref<EntityStore> entityRef, @Nonnull PassiveNode node, @Nonnull PassiveNodeEffect effect) {
        PassiveTreeComponent passiveComponent = entityRef.getStore().getComponent(entityRef, passiveTreeComponentType);
        if (passiveComponent == null) {
            LOGGER.warning("Entity has no PassiveTreeComponent, cannot handle mastery choice from node: " + node.id());
            return;
        }

        List<?> choices = effect.getList("Choices");
        if (choices.isEmpty()) {
            LOGGER.warning("mastery-choice effect has no 'Choices' field on node: " + node.id());
            return;
        }

        // Check if a choice has already been made for this node
        String chosenOptionId = passiveComponent.getMasteryChoice(node.id());
        
        if (chosenOptionId == null) {
            // No choice made yet - mark as pending choice
            // The UI system will detect this and prompt the player
            passiveComponent.markMasteryPending(node.id());
            LOGGER.fine(() -> "Mastery node pending choice: " + node.id());
            return;
        }

        // Apply the effects of the chosen option
        applyChosenOption(entityRef, node, choices, chosenOptionId);
    }

    /**
     * Apply the effects of the chosen mastery option.
     * Called when a choice is made or when re-applying effects (e.g., on login).
     *
     * @param entityRef The entity reference
     * @param node The mastery node
     * @param choices List of choice options from the effect
     * @param chosenOptionId The ID of the chosen option
     */
    public void applyChosenOption(
            @Nonnull Ref<EntityStore> entityRef,
            @Nonnull PassiveNode node,
            @Nonnull List<?> choices,
            @Nonnull String chosenOptionId
    ) {
        Object chosenOption = findOption(choices, chosenOptionId);
        if (chosenOption == null) {
            LOGGER.warning("Chosen option not found: " + chosenOptionId + " on node " + node.id());
            return;
        }

        // Extract effects from the chosen option
        List<PassiveNodeEffect> subEffects = extractEffects(chosenOption);
        
        // Apply each sub-effect using the registry
        PassiveEffectRegistry registry = PassiveEffectRegistry.get();
        for (PassiveNodeEffect subEffect : subEffects) {
            PassiveEffectHandler handler = registry.getHandler(subEffect.type());
            if (handler != null) {
                handler.apply(entityRef, node, subEffect);
            } else {
                LOGGER.warning("No handler for mastery sub-effect type: " + subEffect.type());
            }
        }
        
        LOGGER.fine(() -> "Applied mastery choice: " + chosenOptionId + " on node " + node.id());
    }

    @Override
    public void remove(@Nonnull Ref<EntityStore> entityRef, @Nonnull PassiveNode node, @Nonnull PassiveNodeEffect effect) {
        PassiveTreeComponent passiveComponent = entityRef.getStore().getComponent(entityRef, passiveTreeComponentType);
        if (passiveComponent == null) {
            return;
        }

        List<?> choices = effect.getList("Choices");
        String chosenOptionId = passiveComponent.getMasteryChoice(node.id());
        
        if (chosenOptionId != null && !choices.isEmpty()) {
            // Remove the effects of the chosen option
            Object chosenOption = findOption(choices, chosenOptionId);
            if (chosenOption != null) {
                List<PassiveNodeEffect> subEffects = extractEffects(chosenOption);
                
                PassiveEffectRegistry registry = PassiveEffectRegistry.get();
                for (PassiveNodeEffect subEffect : subEffects) {
                    PassiveEffectHandler handler = registry.getHandler(subEffect.type());
                    if (handler != null) {
                        handler.remove(entityRef, node, subEffect);
                    }
                }
            }
        }
        
        // Clear the choice and pending state
        passiveComponent.clearMasteryChoice(node.id());
        LOGGER.fine(() -> "Removed mastery node: " + node.id());
    }

    @Override
    @Nonnull
    public String getTooltipText(@Nonnull PassiveNodeEffect effect) {
        List<?> choices = effect.getList("Choices");
        if (choices.isEmpty()) {
            return "Mastery: Choose a specialization";
        }
        
        StringBuilder sb = new StringBuilder("Mastery Options:\n");
        for (Object choice : choices) {
            String name = getOptionName(choice);
            String desc = getOptionDescription(choice);
            sb.append("• ").append(name);
            if (desc != null && !desc.isEmpty()) {
                sb.append(": ").append(desc);
            }
            sb.append("\n");
        }
        
        return sb.toString().trim();
    }

    @Nullable
    private Object findOption(List<?> choices, String optionId) {
        for (Object choice : choices) {
            String id = getOptionId(choice);
            if (optionId.equals(id)) {
                return choice;
            }
        }
        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private String getOptionId(Object choice) {
        if (choice instanceof java.util.Map) {
            Object id = ((java.util.Map<String, Object>) choice).get("Id");
            return id instanceof String ? (String) id : null;
        }
        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private String getOptionName(Object choice) {
        if (choice instanceof java.util.Map) {
            Object name = ((java.util.Map<String, Object>) choice).get("Name");
            return name instanceof String ? (String) name : getOptionId(choice);
        }
        return "Unknown";
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private String getOptionDescription(Object choice) {
        if (choice instanceof java.util.Map) {
            Object desc = ((java.util.Map<String, Object>) choice).get("Description");
            return desc instanceof String ? (String) desc : null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    private List<PassiveNodeEffect> extractEffects(Object choice) {
        if (!(choice instanceof java.util.Map)) {
            return List.of();
        }
        
        java.util.Map<String, Object> choiceMap = (java.util.Map<String, Object>) choice;
        Object effectsObj = choiceMap.get("Effects");
        
        if (!(effectsObj instanceof List)) {
            return List.of();
        }
        
        List<?> effectsList = (List<?>) effectsObj;
        java.util.ArrayList<PassiveNodeEffect> results = new java.util.ArrayList<>();
        
        for (Object effectObj : effectsList) {
            if (effectObj instanceof java.util.Map) {
                java.util.Map<String, Object> effectMap = (java.util.Map<String, Object>) effectObj;
                Object typeObj = effectMap.get("Type");
                if (typeObj instanceof String) {
                    String type = (String) typeObj;
                    // Create a new map without the Type field for the data
                    java.util.Map<String, Object> data = new java.util.HashMap<>(effectMap);
                    data.remove("Type");
                    results.add(new PassiveNodeEffect(type, data));
                }
            }
        }
        
        return results;
    }
}

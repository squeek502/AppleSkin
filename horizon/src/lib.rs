//! AppleSkin for Horizon: displays saturation on the hunger HUD and food/saturation values in item tooltips.

use std::borrow::Cow;

use mod_api::client::hud_layout::{Blend, HeartKind};
use mod_api::text::TextStyle;
use mod_api::{
    self, ClientHost, Compound, GuiDraw, HealthHudEvent, HealthInputs, Hooks, ItemTooltipEvent,
    Tag, Text,
};

const WHITE: u32 = 0xFFFF_FFFF;
const GOLD_ARGB: u32 = 0xFFFF_D700;
const GOLD_RGBA: u32 = 0xFFAA_00FF;

fn sprite(name: &'static str, x: i32, y: i32) -> GuiDraw {
    GuiDraw::Sprite {
        sprite: Cow::Borrowed(name),
        x,
        y,
        w: 9,
        h: 9,
        region: None,
        color: WHITE,
        blend: Blend::Textured,
    }
}

fn heart_sprites(kind: HeartKind, hardcore: bool, absorbing: bool) -> (&'static str, &'static str) {
    if absorbing {
        return if hardcore {
            ("hud/heart/absorbing_hardcore_full", "hud/heart/absorbing_hardcore_half")
        } else {
            ("hud/heart/absorbing_full", "hud/heart/absorbing_half")
        };
    }
    match (kind, hardcore) {
        (HeartKind::Poisoned, true) => ("hud/heart/poisoned_hardcore_full", "hud/heart/poisoned_hardcore_half"),
        (HeartKind::Poisoned, false) => ("hud/heart/poisoned_full", "hud/heart/poisoned_half"),
        (HeartKind::Withered, true) => ("hud/heart/withered_hardcore_full", "hud/heart/withered_hardcore_half"),
        (HeartKind::Withered, false) => ("hud/heart/withered_full", "hud/heart/withered_half"),
        (HeartKind::Frozen, true) => ("hud/heart/frozen_hardcore_full", "hud/heart/frozen_hardcore_half"),
        (HeartKind::Frozen, false) => ("hud/heart/frozen_full", "hud/heart/frozen_half"),
        (HeartKind::Normal, true) => ("hud/heart/hardcore_full", "hud/heart/hardcore_half"),
        (HeartKind::Normal, false) => ("hud/heart/full", "hud/heart/half"),
    }
}

fn compound_get<'a>(map: &'a Compound, key: &str) -> Option<&'a Tag> {
    map.iter()
        .find(|(k, _)| k.to_string().ok().as_deref() == Some(key))
        .map(|(_, v)| v)
}

fn tag_as_i32(tag: &Tag) -> Option<i32> {
    match tag {
        Tag::Int(v) => Some(*v),
        Tag::Short(v) => Some(*v as i32),
        Tag::Byte(v) => Some(*v as i32),
        _ => None,
    }
}

fn tag_as_f32(tag: &Tag) -> Option<f32> {
    match tag {
        Tag::Float(bits) => Some(f32::from_bits(*bits)),
        Tag::Double(bits) => Some(f64::from_bits(*bits) as f32),
        Tag::Int(v) => Some(*v as f32),
        _ => None,
    }
}

/// Extracts `(nutrition, saturation)` from a `minecraft:food` component compound.
fn extract_food_stats(nbt: &Compound) -> Option<(i32, f32)> {
    let Tag::Compound(food) = compound_get(nbt, "minecraft:food")? else {
        return None;
    };
    let nutrition = compound_get(food, "nutrition").and_then(tag_as_i32)?;
    let saturation = compound_get(food, "saturation").and_then(tag_as_f32).unwrap_or(0.0);
    Some((nutrition, saturation))
}

/// Draws a golden saturation outline around a food icon at `(x, y)`.
fn push_saturation_outline(draws: &mut Vec<GuiDraw>, x: i32, y: i32, half_only: bool) {
    let w = if half_only { 5 } else { 9 };
    let x_start = if half_only { x + 4 } else { x };
    // Top and bottom 1px gold bars
    draws.push(GuiDraw::Fill {
        x0: x_start,
        y0: y,
        x1: x_start + w,
        y1: y + 1,
        color: GOLD_ARGB,
    });
    draws.push(GuiDraw::Fill {
        x0: x_start,
        y0: y + 8,
        x1: x_start + w,
        y1: y + 9,
        color: GOLD_ARGB,
    });
}

fn build_appleskin_hud(inputs: &HealthInputs, wi: i32, hi: i32) -> Vec<GuiDraw> {
    let mut draws = Vec::new();
    let left_x = wi / 2 - 91;
    let right_x = wi / 2 + 91;
    let base_y = hi - 39;

    // 1. Player health + absorption
    let health = (inputs.health.ceil() as i32).max(0);
    let max_health = (inputs.max_health.ceil() as i32).max(0);
    let absorption = (inputs.absorption.ceil() as i32).max(0);
    let total_hearts = (max_health + absorption + 1) / 2;
    let max_health_hearts = (max_health + 1) / 2;
    let health_rows = ((total_hearts + 9) / 10).max(1);
    let row_height = (10 - (health_rows - 2)).max(3);
    let container = if inputs.hardcore {
        "hud/heart/container_hardcore"
    } else {
        "hud/heart/container"
    };

    for idx in (0..total_hearts).rev() {
        let row = idx / 10;
        let col = idx % 10;
        let x = left_x + col * 8;
        let y = base_y - row * row_height;
        draws.push(sprite(container, x, y));

        if idx >= max_health_hearts {
            let abs_half = idx * 2 - max_health_hearts * 2;
            let (full, half) = heart_sprites(inputs.kind, inputs.hardcore, true);
            if abs_half + 1 < absorption {
                draws.push(sprite(full, x, y));
            } else if abs_half + 1 == absorption {
                draws.push(sprite(half, x, y));
            }
        } else {
            let (full, half) = heart_sprites(inputs.kind, inputs.hardcore, false);
            if idx * 2 + 1 < health {
                draws.push(sprite(full, x, y));
            } else if idx * 2 + 1 == health {
                draws.push(sprite(half, x, y));
            }
        }
    }

    // 2. Player armor
    if inputs.armor > 0 {
        let armor_y = base_y - (health_rows - 1) * row_height - 10;
        for i in 0..10 {
            let x = left_x + i * 8;
            let tex = if i * 2 + 1 < inputs.armor {
                "hud/armor_full"
            } else if i * 2 + 1 == inputs.armor {
                "hud/armor_half"
            } else {
                "hud/armor_empty"
            };
            draws.push(sprite(tex, x, armor_y));
        }
    }

    // 3. Food bar + AppleSkin saturation overlay
    let (food_empty, food_full, food_half) = if inputs.hunger {
        ("hud/food_empty_hunger", "hud/food_full_hunger", "hud/food_half_hunger")
    } else {
        ("hud/food_empty", "hud/food_full", "hud/food_half")
    };
    let sat_half_icons = (inputs.saturation.max(0.0).ceil() as i32).min(20);
    for i in 0..10 {
        let x = right_x - i * 8 - 9;
        draws.push(sprite(food_empty, x, base_y));
        if i * 2 + 1 < inputs.food {
            draws.push(sprite(food_full, x, base_y));
        } else if i * 2 + 1 == inputs.food {
            draws.push(sprite(food_half, x, base_y));
        }
        if i * 2 + 1 < sat_half_icons {
            push_saturation_outline(&mut draws, x, base_y, false);
        } else if i * 2 + 1 == sat_half_icons {
            push_saturation_outline(&mut draws, x, base_y, true);
        }
    }

    // 4. Air bubbles
    if inputs.under_water || inputs.air < inputs.max_air {
        let air_y = base_y - 10;
        let max_air = inputs.max_air.max(1);
        let full_bubbles = ((inputs.air.max(0) * 10) + max_air - 1) / max_air;
        let partial = (inputs.air.max(0) * 10) / max_air;
        for i in 0..10 {
            let x = right_x - i * 8 - 9;
            if i < partial {
                draws.push(sprite("hud/air", x, air_y));
            } else if i < full_bubbles {
                draws.push(sprite("hud/air_bursting", x, air_y));
            } else {
                draws.push(sprite("hud/air_empty", x, air_y));
            }
        }
    }

    draws
}

#[derive(Default)]
struct AppleSkinMod;

#[mod_api::horizon_mod]
impl Hooks for AppleSkinMod {
    fn health_hud(&self, _host: &mut dyn ClientHost, event: &mut HealthHudEvent<'_>) -> Option<Vec<GuiDraw>> {
        if event.inputs.vehicle_hearts > 0 || event.inputs.saturation <= 0.0 {
            return None;
        }
        Some(build_appleskin_hud(event.inputs, event.wi, event.hi))
    }

    fn item_tooltip(&self, _host: &dyn ClientHost, event: &mut ItemTooltipEvent<'_>) {
        let Some(components) = event.stack.components() else {
            return;
        };
        let Some((nutrition, saturation)) = extract_food_stats(components.as_nbt()) else {
            return;
        };
        let mut style = TextStyle::default();
        style.color = Some("gold".to_string());
        let line = Text::literal(format!("Hunger: +{nutrition}  Saturation: +{saturation:.1}"))
            .with_style(style);
        event.lines.push((line, GOLD_RGBA));
    }
}

#[cfg(test)]
mod tests {
    use super::build_appleskin_hud;
    use mod_api::client::hud_layout::HeartKind;
    use mod_api::{GuiDraw, HealthInputs};

    #[test]
    fn draws_gold_saturation_bars_when_saturated() {
        let inputs = HealthInputs {
            tick: 0,
            millis: 0,
            health: 20.0,
            max_health: 20.0,
            absorption: 0.0,
            armor: 0,
            food: 20,
            saturation: 6.0,
            kind: HeartKind::Normal,
            regeneration: false,
            hunger: false,
            hardcore: false,
            damage_cooldown: false,
            air: 300,
            max_air: 300,
            under_water: false,
            vehicle_hearts: 0,
        };
        let draws = build_appleskin_hud(&inputs, 427, 240);
        assert!(draws.iter().any(|d| matches!(d, GuiDraw::Fill { .. })));
    }
}

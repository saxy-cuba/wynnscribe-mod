package com.wynnscribe.wynntils

import com.wynnscribe.DeveloperUtils
import com.wynnscribe.MiniMessage
import com.wynnscribe.Translator
import com.wynntils.mc.event.ItemTooltipRenderEvent
import com.wynntils.mc.extension.ItemStackExtension
import com.wynntils.models.items.items.game.*
import com.wynntils.models.items.items.gui.ServerItem
import com.wynntils.models.npcdialogue.event.NpcDialogueProcessingEvent
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent

class EventHandler {
    @SubscribeEvent(receiveCanceled = true, priority = EventPriority.LOW)
    fun on(event: ItemTooltipRenderEvent.Pre) {
        DeveloperUtils.lastHoveredLore = event.tooltips
        // ItemStackのWynntilsの形式に変更
        val extension = event.itemStack as ItemStackExtension
        // annotationはWynntilsによって推定されたアイテムの種類です。
        val annotation = extension.annotation
        when(annotation) {
            // >>> 武器や防具だった場合 >>>
            is GearItem -> {
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.gear")
                return
            }
            is CraftedGearItem -> {
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.gear")
                return
            }
            is UnknownGearItem -> {
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.gear")
                return
            }
            // <<< 武器や防具だった場合 <<<
            // 未鑑定のボックスだった場合
            is GearBoxItem -> {
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.gear-box")
                return
            }
            // >>> Corkian系列 >>>
            is AmplifierItem -> {
                // Corkian Amplifierの場合 https://wynncraft.fandom.com/wiki/Corkian_Amplifier
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.corkian-amplifier")
                return
            }
            is SimulatorItem -> {
                // https://wynncraft.fandom.com/wiki/Corkian_Simulator
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.corkian-simulator")
                return
            }
            is InsulatorItem -> {
                // https://wynncraft.fandom.com/wiki/Corkian_Insulator
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.corkian-insulator")
                return
            }
            // <<< Corkian 系列 <<<

            is AspectItem -> {
                // Aspects　https://wynncraft.wiki.gg/wiki/Aspects
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.aspect")
                return
            }
            is CharmItem -> {
                // Charm https://wynncraft.fandom.com/wiki/Charms
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.charm")
                return
            }
            is CorruptedCacheItem -> {
                // https://wynncraft.wiki.gg/wiki/Corrupted_Cache
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.corrupted-cache")
                return
            }
            is CraftedConsumableItem -> {
                // 謎
            }

            is DungeonKeyItem -> {
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.dungeon-key")
                return
                // ダンジョンのカギ
            }
            is EmeraldItem -> {
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.emerald")
                return
            }
            is EmeraldPouchItem -> {
                // エメラルドポーチ
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.emerald-pouch")
                return
            }
            is GatheringToolItem -> {
                // 採取ツール
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.gathering-tool")
                return
            }
            is HorseItem -> {
                // 馬
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.horse")
                return
            }
            is MaterialItem -> {
                // クラフト材料とかいろいろ(主に採取ツールで採取する系
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.material")
                return
            }
            is IngredientItem -> {
                // クラフト素材とかいろいろ(主に敵ドロップ系
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.ingredient")
                return
            }
            is MultiHealthPotionItem -> {
                // ポーションをまとめたやつ
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.multi-health-potion")
                return
            }
            is PotionItem -> {
                // ポーション系
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.potion")
                return
            }
            is PowderItem -> {
                // パウダー系
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.powder")
                return
            }
            is RuneItem -> {
                // ルーン系
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.rune")
                return
            }
            is TeleportScrollItem -> {
                // テレポートスクロール
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.teleport-scroll")
                return
            }
            is TomeItem -> {
                // https://wynncraft.fandom.com/wiki/Mastery_Tomes
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.item.tome")
                return
            }
            is ServerItem -> {
                // サーバーセレクター
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, "#wynnscribe.gui.server")
                return
            }
        }
        // どのアイテムでもなかった場合は普通にフィルタリングをする
        event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, null)
    }

    private val DialogueProgressionRegex = Regex("^<cv1><[^<>\\[\\]]+>\\[(\\d+)/(\\d+)]")

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun on(event: NpcDialogueProcessingEvent.Pre) {
        val tags = mapOf(
            "<!italic><!underlined><!strikethrough><!bold><!obfuscated><font:default>" to "<cv1>",
            "</font></!obfuscated></!bold></!strikethrough></!underlined></!italic>" to "</cv1>"
        )
        var message = MiniMessage.serializeList(event.dialogue.dialogueComponent().map(MinecraftClientAudiences.of()::asAdventure))
        for ((key, value) in tags) {
            message = message.replace(key, value)
        }
        val match = DialogueProgressionRegex.find(message)
        if(match != null && match.groupValues.size == 3) {
            val now = match.groupValues[1].toIntOrNull()
            val total = match.groupValues[2].toIntOrNull()
            if(now != null && total != null) {
                println("ああああああああああああああああああああああああああ: ${now}/${total}")
            }
        }
        println(message)
    }
}
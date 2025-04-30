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
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
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
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.gear"), event.tooltips.firstOrNull())
                return
            }
            is CraftedGearItem -> {
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.gear"), event.tooltips.firstOrNull())
                return
            }
            is UnknownGearItem -> {
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.gear"), event.tooltips.firstOrNull())
                return
            }
            // <<< 武器や防具だった場合 <<<
            // 未鑑定のボックスだった場合
            is GearBoxItem -> {
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.gear-box"), event.tooltips.firstOrNull())
                return
            }
            // >>> Corkian系列 >>>
            is AmplifierItem -> {
                // Corkian Amplifierの場合 https://wynncraft.fandom.com/wiki/Corkian_Amplifier
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.corkian-amplifier"), event.tooltips.firstOrNull())
                return
            }
            is SimulatorItem -> {
                // https://wynncraft.fandom.com/wiki/Corkian_Simulator
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.corkian-simulator"), event.tooltips.firstOrNull())
                return
            }
            is InsulatorItem -> {
                // https://wynncraft.fandom.com/wiki/Corkian_Insulator
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.corkian-insulator"), event.tooltips.firstOrNull())
                return
            }
            // <<< Corkian 系列 <<<

            is AspectItem -> {
                // Aspects　https://wynncraft.wiki.gg/wiki/Aspects
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.aspect"), event.tooltips.firstOrNull())
                return
            }
            is CharmItem -> {
                // Charm https://wynncraft.fandom.com/wiki/Charms
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.charm"), event.tooltips.firstOrNull())
                return
            }
            is CorruptedCacheItem -> {
                // https://wynncraft.wiki.gg/wiki/Corrupted_Cache
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.corrupted-cache"), event.tooltips.firstOrNull())
                return
            }
            is CraftedConsumableItem -> {
                // 謎
            }

            is DungeonKeyItem -> {
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.dungeon-key"), event.tooltips.firstOrNull())
                return
                // ダンジョンのカギ
            }
            is EmeraldItem -> {
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.emerald"), event.tooltips.firstOrNull())
                return
            }
            is EmeraldPouchItem -> {
                // エメラルドポーチ
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.emerald-pouch"), event.tooltips.firstOrNull())
                return
            }
            is GatheringToolItem -> {
                // 採取ツール
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.gathering-tool"), event.tooltips.firstOrNull())
                return
            }
            is HorseItem -> {
                // 馬
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.horse"), event.tooltips.firstOrNull())
                return
            }
            is MaterialItem -> {
                // クラフト材料とかいろいろ(主に採取ツールで採取する系
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.material"), event.tooltips.firstOrNull())
                return
            }
            is IngredientItem -> {
                // クラフト素材とかいろいろ(主に敵ドロップ系
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.ingredient"), event.tooltips.firstOrNull())
                return
            }
            is MultiHealthPotionItem -> {
                // ポーションをまとめたやつ
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.multi-health-potion"), event.tooltips.firstOrNull())
                return
            }
            is PotionItem -> {
                // ポーション系
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.potion"), event.tooltips.firstOrNull())
                return
            }
            is PowderItem -> {
                // パウダー系
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.powder"), event.tooltips.firstOrNull())
                return
            }
            is RuneItem -> {
                // ルーン系
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.rune"), event.tooltips.firstOrNull())
                return
            }
            is TeleportScrollItem -> {
                // テレポートスクロール
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.teleport-scroll"), event.tooltips.firstOrNull())
                return
            }
            is TomeItem -> {
                // https://wynncraft.fandom.com/wiki/Mastery_Tomes
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.item.tome"), event.tooltips.firstOrNull())
                return
            }
            is ServerItem -> {
                // サーバーセレクター
                event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Component.literal("#wynnscribe.gui.server"), event.tooltips.firstOrNull())
                return
            }
        }
        // どのアイテムでもなかった場合は普通にフィルタリングをする
        event.tooltips = Translator.translateItemStackOrCached(event.itemStack, event.tooltips, Minecraft.getInstance().screen?.title, event.tooltips.firstOrNull())
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
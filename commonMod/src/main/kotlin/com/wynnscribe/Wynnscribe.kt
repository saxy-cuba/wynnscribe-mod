package com.wynnscribe

import com.mojang.blaze3d.platform.InputConstants
import com.wynnscribe.wynntils.EventHandler
import com.wynntils.core.WynntilsMod
import dev.architectury.event.events.client.ClientLifecycleEvent
import dev.architectury.platform.Platform
import dev.architectury.registry.ReloadListenerRegistry
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

object Wynnscribe {

    const val MOD_ID = "wynnscribe"

    const val STUDIO_HOST = "http://192.168.0.7:5173"

    val P = KeyMapping("key.wynnscribe.p", InputConstants.Type.KEYSYM, InputConstants.KEY_P, "category.wynnscribe.debug")
    val O = KeyMapping("key.wynnscribe.o", InputConstants.Type.KEYSYM, InputConstants.KEY_O, "category.wynnscribe.debug")

    fun init() {
        KeyMappings.register(P, KeyMappings.RegisterType.INVENTORY) {
            val screen = Minecraft.getInstance().screen
            if (screen is AbstractContainerScreen<*>) {
                println("================")
                val serialized = MiniMessage.miniMessage().serialize(MinecraftClientAudiences.of().asAdventure(Minecraft.getInstance().screen!!.getTitle()))
                println(serialized)
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                val selection = StringSelection(serialized)
                clipboard.setContents(selection, selection)
                println("Copied to clipboard")
            }
        }

        KeyMappings.register(O, KeyMappings.RegisterType.INVENTORY) {
            try {
                Minecraft.getInstance().user?.accessToken
                val screen = Minecraft.getInstance().screen
                if (screen is AbstractContainerScreen<*>) {
                    println("================")
                    val lore = DeveloperUtils.lastHoveredLore.map(MinecraftClientAudiences.of()::asAdventure)
                        .joinToString("\n", transform = com.wynnscribe.MiniMessage::serialize)
                    val serialized = mapOf("text" to lore, "filter.title.content" to lore.split("\n").first())

                    println("---")
                    println(
                        DeveloperUtils.lastHoveredLore.map(MinecraftClientAudiences.of()::asAdventure)
                            .joinToString("\n", transform = MiniMessage.miniMessage()::serialize)
                    )

                    println("===")
                    println(lore)
                    println("===")

                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    val selection = StringSelection(lore)
                    clipboard.setContents(selection, selection)
                    println("Copied to clipboard")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        ClientLifecycleEvent.CLIENT_STARTED.register { server ->
            if(Platform.isModLoaded("wynntils")) {
                WynntilsMod.registerEventListener(EventHandler())
            }
        }

        val translationResourceLocation = ResourceLocation.tryBuild(MOD_ID, "translations")
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, object: PreparableReloadListener {
            override fun reload(
                state: PreparableReloadListener.PreparationBarrier,
                resourceManager: ResourceManager,
                backgroundExecutor: Executor,
                gameExecutor: Executor
            ): CompletableFuture<Void> {
                val prepareFuture = CompletableFuture.supplyAsync({
                    return@supplyAsync runCatching { API.loadOrDownloadTranslations(Minecraft.getInstance().languageManager.selected) }
                },backgroundExecutor)
                return prepareFuture.thenCompose(state::wait).thenAcceptAsync({ projectsResult ->
                    Translator.caches.clear()
                    Translator.history = listOf()
                    Translator.Translation = projectsResult.getOrNull()
                }, gameExecutor)
            }
        }, translationResourceLocation)
    }
}
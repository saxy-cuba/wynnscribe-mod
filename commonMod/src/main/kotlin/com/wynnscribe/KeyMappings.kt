package com.wynnscribe

import com.wynnscribe.KeyMappings.RegisterType.*
import dev.architectury.event.events.client.ClientTickEvent
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import java.util.function.Consumer

object KeyMappings {
    class RegisteredKeyMapping(private val keyMapping: KeyMapping, private val runnable: ()-> Unit) {
        fun matches(keyCode: Int, scanCode: Int): Boolean {
            return this.keyMapping.matches(keyCode, scanCode)
        }

        fun consumeClick(): Boolean {
            return this.keyMapping.consumeClick()
        }

        fun run() {
            this.runnable.invoke()
        }
    }

    private val defaultMappings = ArrayList<RegisteredKeyMapping?>()

    private val inventoryMappings = ArrayList<RegisteredKeyMapping?>()

    fun handleClick(keyCode: Int, scanCode: Int) {
        this.inventoryMappings.forEach(Consumer { keyMapping: RegisteredKeyMapping? ->
            if (keyMapping!!.matches(keyCode, scanCode)) {
                keyMapping.run()
            }
        })
    }


    fun register(keyMapping: KeyMapping, vararg types: RegisterType, runnable: () -> Unit) {
        types.distinct().forEach { type ->
            when(type) {
                DEFAULT -> this.defaultMappings.add(RegisteredKeyMapping(keyMapping, runnable))
                INVENTORY -> this.inventoryMappings.add(RegisteredKeyMapping(keyMapping, runnable))
            }
        }
    }

    enum class RegisterType {
        DEFAULT,
        INVENTORY
    }

    init {
        ClientTickEvent.CLIENT_POST.register { client ->
            this.defaultMappings.forEach(Consumer { keyMapping: RegisteredKeyMapping? ->
                if (keyMapping!!.consumeClick()) {
                    keyMapping.run()
                }
            })
        }
    }
}
package com.jamieswhiteshirt.developermode.mixin.client.gui.screen.world;

import com.jamieswhiteshirt.developermode.DeveloperMode;
import com.jamieswhiteshirt.developermode.client.DeveloperModeClient;
import com.jamieswhiteshirt.developermode.client.ItemImageExporter;
import com.jamieswhiteshirt.developermode.client.NewLevelProperties;
import com.jamieswhiteshirt.developermode.client.gui.screen.world.GameRulesScreen;
import com.jamieswhiteshirt.developermode.client.gui.screen.world.CreateWorldScreenExtension;
import com.jamieswhiteshirt.developermode.common.world.level.LevelInfoExtension;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.GameRules;
import net.minecraft.world.level.LevelGeneratorType;
import net.minecraft.world.level.LevelInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.*;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin extends Screen implements CreateWorldScreenExtension {
    private File developermode_propertiesFile;
    private GameRules developermode_gameRules = new GameRules();
    private ButtonWidget developermode_gameRulesButton;

    @Shadow private String seed;
    @Shadow private int generatorType;
    @Shadow public CompoundTag generatorOptionsTag;
    @Shadow private boolean structures;
    @Shadow private boolean commandsAllowed;
    @Shadow private String gameMode;
    @Shadow private boolean enableBonusItems;
    @Shadow private String levelName;

    protected CreateWorldScreenMixin(Component textComponent_1) {
        super(textComponent_1);
    }

    @Inject(
        at = @At("RETURN"),
        method = "removed()V"
    )
    private void removed(CallbackInfo ci) {
        if (developermode_propertiesFile != null) {
            developermode_writeLevelPropertiesFile();
        }
    }

    @ModifyVariable(
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelInfo;setGeneratorOptions(Lcom/google/gson/JsonElement;)Lnet/minecraft/world/level/LevelInfo;"
        ),
        method = "createLevel()V"
    )
    private LevelInfo modifyLevelInfo(LevelInfo original) {
        LevelInfoExtension extension = (LevelInfoExtension) (Object) original;
        extension.developermode_setGameRules(developermode_gameRules);
        return original;
    }

    @Inject(
        method = "init()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/world/CreateWorldScreen;method_2710(Z)V"
        )
    )
    private void init(CallbackInfo ci) {
        developermode_gameRulesButton = this.addButton(new ButtonWidget(2 * this.width / 3 + 5, 187, 80, 20, I18n.translate("developermode.gameRules"), (buttonWidget_1) -> {
            try (ItemImageExporter exporter = new ItemImageExporter(256, minecraft.getItemRenderer())) {
                exporter.render(new ItemStack(Blocks.BLUE_STAINED_GLASS), new File("blue_stained_glass.png"));
            }
            minecraft.openScreen(new GameRulesScreen(developermode_gameRules, (CreateWorldScreen) (Object) this));
        }));
    }

    @Inject(
        method = "method_2710(Z)V",
        at = @At("TAIL")
    )
    private void method_2710(boolean showMoreOptions, CallbackInfo ci) {
        developermode_gameRulesButton.visible = showMoreOptions && DeveloperModeClient.gameRulesGuiEnabled;
    }

    @Override
    public void developermode_setLevelPropertiesFile(File file) {
        developermode_propertiesFile = file;
        if (developermode_propertiesFile.exists()) {
            developermode_readLevelPropertiesFile();
        } else {
            developermode_writeLevelPropertiesFile();
        }
    }

    @Override
    public void developermode_setLevelName(String name) {
        levelName = name;
    }

    private void developermode_readLevelPropertiesFile() {
        NewLevelProperties properties = new NewLevelProperties();
        try {
            properties.readFromFile(developermode_propertiesFile);
            developermode_setProperties(properties);
        } catch (IOException e) {
            DeveloperMode.LOGGER.error("Could not read new world settings", e);
        }
    }

    private void developermode_writeLevelPropertiesFile() {
        NewLevelProperties properties = developermode_getProperties();
        try {
            properties.writeToFile(developermode_propertiesFile);
        } catch (IOException e) {
            DeveloperMode.LOGGER.error("Could not write new world settings", e);
        }
    }

    private NewLevelProperties developermode_getProperties() {
        NewLevelProperties properties = new NewLevelProperties();
        properties.randomSeed = seed;
        properties.generatorName = LevelGeneratorType.TYPES[generatorType].getName();
        properties.generatorOptions = (CompoundTag) generatorOptionsTag.copy();
        properties.mapFeatures = structures;
        properties.allowCommands = commandsAllowed;
        properties.bonusItems = enableBonusItems;
        properties.gameType = gameMode;
        properties.gameRules = developermode_gameRules;
        return properties;
    }

    private void developermode_setProperties(NewLevelProperties properties) {
        seed = properties.randomSeed;
        LevelGeneratorType levelGeneratorType = LevelGeneratorType.getTypeFromName(properties.generatorName);
        generatorType = levelGeneratorType != null ? levelGeneratorType.getId() : 0;
        generatorOptionsTag = (CompoundTag) properties.generatorOptions.copy();
        structures = properties.mapFeatures;
        commandsAllowed = properties.allowCommands;
        enableBonusItems = properties.bonusItems;
        gameMode = properties.gameType;
        developermode_gameRules = properties.gameRules;
    }
}

package de.brickforceaurora.launcher.platform;

import de.brickforceaurora.launcher.CondConstant;
import de.brickforceaurora.launcher.LauncherApp;
import de.brickforceaurora.launcher.animation.property.PropFloat;
import de.brickforceaurora.launcher.animation.property.PropString;
import de.brickforceaurora.launcher.platform.linux.LinuxPlatform;
import de.brickforceaurora.launcher.platform.windows.WindowsPlatform;
import me.lauriichan.laylib.command.Actor;

public abstract class Platform {
    
    public static final Platform createPlatform() {
        switch(CondConstant.OS) {
        case LINUX:
            return new LinuxPlatform(LauncherApp.get());
        case WINDOWS:
            return new WindowsPlatform(LauncherApp.get());
        default:
            throw new IllegalStateException("Unsupported OS");
        }
    }
    
    public boolean needsPlatformSetup() {
        return false;
    }

    public boolean doPlatformSetup(PropString mainText, PropFloat mainProgress) throws Exception {
        return true;
    }

    public abstract void applyLanguageFix(Actor<?> actor);

    public abstract void applyWindowedFix(Actor<?> actor);

    public abstract boolean updateLauncher();

    public abstract void startGame();

}

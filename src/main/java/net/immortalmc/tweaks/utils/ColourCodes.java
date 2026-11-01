package net.immortalmc.tweaks.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ColourCodes {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private ColourCodes() {}

    /** Converts text containing & codes and #rrggbb hex codes into a Component. */
    public static Component parse(String input) {
        Component result = LEGACY.deserialize(input);
        String reEncoded = MINI.serialize(result);

        String withHex = reEncoded.replaceAll("#([A-Fa-f0-9]{6})", "<color:#$1>");

        return MINI.deserialize(withHex);
    }

    /** Converts a Component back into the & and #rrggbb codes a player would type. */
    public static String toCodes(Component component) {
        String miniMessage = MINI.serialize(component);

        String withHex = miniMessage.replaceAll("<color:(#[A-Fa-f0-9]{6})>", "$1");
        withHex = withHex.replaceAll("<(#[A-Fa-f0-9]{6})>", "$1");
        withHex = withHex.replace("</color>", "");
        withHex = withHex
            .replace("<black>", "&0").replace("<dark_blue>", "&1")
            .replace("<dark_green>", "&2").replace("<dark_aqua>", "&3")
            .replace("<dark_red>", "&4").replace("<dark_purple>", "&5")
            .replace("<gold>", "&6").replace("<gray>", "&7")
            .replace("<dark_gray>", "&8").replace("<blue>", "&9")
            .replace("<green>", "&a").replace("<aqua>", "&b")
            .replace("<red>", "&c").replace("<light_purple>", "&d")
            .replace("<yellow>", "&e").replace("<white>", "&f")
            .replace("<bold>", "&l").replace("</bold>", "")
            .replace("<italic>", "&o").replace("</italic>", "")
            .replace("<underlined>", "&n").replace("</underlined>", "")
            .replace("<strikethrough>", "&m").replace("</strikethrough>", "")
            .replace("<obfuscated>", "&k").replace("</obfuscated>", "")
            .replace("<reset>", "&r");

        return withHex;
    }

    /** Builds a line of text where each code is highlighted, hoverable and click-to-copy. */
    public static Component buildCodeLine(String codes) {
        Component line = Component.empty();
        int i = 0;

        while (i < codes.length()) {
            if (i < codes.length() - 1 && codes.charAt(i) == '&') {
                char code = Character.toLowerCase(codes.charAt(i + 1));
                String token = "&" + codes.charAt(i + 1);
                TextColor tokenColor = legacyCodeToColor(code);
                TextDecoration decoration = legacyCodeToDecoration(code);

                Component tokenComponent = Component.text(token)
                    .color(tokenColor != null ? tokenColor : TextColor.color(0xFFFFFF))
                    .clickEvent(ClickEvent.copyToClipboard(token))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to copy " + token + " to clipboard")));

                if (decoration != null) {
                    tokenComponent = tokenComponent.decorate(decoration);
                }

                line = line.append(tokenComponent);
                i += 2;
            } else if (i + 7 <= codes.length() && codes.charAt(i) == '#') {
                String hex = codes.substring(i, i + 7);
                try {
                    TextColor tokenColor = TextColor.fromHexString(hex);
                    line = line.append(Component.text(hex)
                        .color(tokenColor != null ? tokenColor : TextColor.color(0xFFFFFF))
                        .clickEvent(ClickEvent.copyToClipboard(hex))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to copy " + hex + " to clipboard"))));
                    i += 7;
                } catch (Exception e) {
                    line = line.append(Component.text(String.valueOf(codes.charAt(i))));
                    i++;
                }
            } else {
                line = line.append(Component.text(String.valueOf(codes.charAt(i)))
                    .color(TextColor.color(0xFFFFFF)));
                i++;
            }
        }

        return line;
    }

    private static TextColor legacyCodeToColor(char code) {
        return switch (code) {
            case '0' -> TextColor.color(0x000000);
            case '1' -> TextColor.color(0x0000AA);
            case '2' -> TextColor.color(0x00AA00);
            case '3' -> TextColor.color(0x00AAAA);
            case '4' -> TextColor.color(0xAA0000);
            case '5' -> TextColor.color(0xAA00AA);
            case '6' -> TextColor.color(0xFFAA00);
            case '7' -> TextColor.color(0xAAAAAA);
            case '8' -> TextColor.color(0x555555);
            case '9' -> TextColor.color(0x5555FF);
            case 'a' -> TextColor.color(0x55FF55);
            case 'b' -> TextColor.color(0x55FFFF);
            case 'c' -> TextColor.color(0xFF5555);
            case 'd' -> TextColor.color(0xFF55FF);
            case 'e' -> TextColor.color(0xFFFF55);
            case 'f' -> TextColor.color(0xFFFFFF);
            default -> null;
        };
    }

    private static TextDecoration legacyCodeToDecoration(char code) {
        return switch (code) {
            case 'l' -> TextDecoration.BOLD;
            case 'm' -> TextDecoration.STRIKETHROUGH;
            case 'n' -> TextDecoration.UNDERLINED;
            case 'o' -> TextDecoration.ITALIC;
            default -> null;
        };
    }
}
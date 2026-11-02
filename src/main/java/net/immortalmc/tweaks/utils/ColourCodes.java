package net.immortalmc.tweaks.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts between the colour codes a player types (&a, &l, #rrggbb) and Adventure Components.
 *
 *   parse()           player-typed text -> Component (used when applying names and sign text)
 *   toCodes()         Component -> typed codes (used to show a player what a name contains)
 *   buildCodeLine()   typed codes -> clickable, highlighted chat line
 */
public final class ColourCodes {

    // Reads & codes, plus hex colours in the &#rrggbb form. 
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
        .character('&')
        .hexCharacter('#')
        .hexColors()
        .build();

    // Reads hex codes in the #rrggbb form (without & prefix)
    private static final String HEX_PATTERN = "#([A-Fa-f0-9]{6})";
    private static final int HEX_LENGTH = 7; // "#" plus 6 hex digits

    // Every character that can follow an & to make a valid code (colours, formatting, k = obfuscated, r = reset).
    private static final String LEGACY_CODES = "0123456789abcdefklmnor";

    private static final TextColor WHITE = NamedTextColor.WHITE;
    private static final int NO_COLOUR = -1;

    // Convert colour value to the appropriate & code for it. Anything else is written as hex.
    private static final Map<Integer, Character> COLOUR_CODES = new HashMap<>();
    static {
        for (char code : "0123456789abcdef".toCharArray()) {
            COLOUR_CODES.put(legacyCodeToColor(code).value(), code);
        }
    }

    // A segment is a piece of text with its colour and formatting information
    private record Segment(String text, Style style) {}

    private ColourCodes() {}

    // Converts text containing & codes and #rrggbb hex codes into a Component.
    public static Component parse(String input) {
        // The serializer reads hex as &#rrggbb, so add the & to any plain #rrggbb codes.
        return LEGACY.deserialize(input.replaceAll("(?<!&)" + HEX_PATTERN, "&#$1"));
    }

    // Converts a Component back into the & and #rrggbb codes as a player would type it.
    public static String toCodes(Component component) {
        List<Segment> segments = new ArrayList<>();
        collect(component, Style.empty(), segments);

        StringBuilder codes = new StringBuilder();

        // The colour and formatting that someone reading the codes written so far would see.
        // Both start empty because nothing has been written yet, and are updated as codes are added
        int activeColour = NO_COLOUR;
        Set<TextDecoration> activeDecorations = EnumSet.noneOf(TextDecoration.class);

        for (Segment segment : segments) {
            TextColor wantedColour = segment.style().color();
            int wantedValue = wantedColour == null ? NO_COLOUR : wantedColour.value();
            Set<TextDecoration> wantedDecorations = decorationsOf(segment.style());

            // Codes can only switch things on, so we need to manually clear old formatting
            boolean formattingMustStop = !wantedDecorations.containsAll(activeDecorations);

            if (wantedValue == NO_COLOUR) {
                // Only a reset (&r) can remove a colour or formatting
                if (activeColour != NO_COLOUR || formattingMustStop) {
                    codes.append("&r");
                    activeColour = NO_COLOUR;
                    activeDecorations.clear();
                }
            } else if (wantedValue != activeColour || formattingMustStop) {
                // A colour code also clears formatting, so no &r is needed.
                // This also covers "same colour, less formatting": the colour is simply written again.
                codes.append(colourCode(wantedColour));
                activeColour = wantedValue;
                activeDecorations.clear();
            }

            // Add any formatting that isn't active yet
            for (TextDecoration decoration : wantedDecorations) {
                if (activeDecorations.add(decoration)) {
                    codes.append('&').append(decorationCode(decoration));
                }
            }

            codes.append(segment.text());
        }

        return codes.toString();
    }

    /**
     * Flattens a component and its children into plain text pieces, each with the style it actually displays with. 
     * A child inherits its parent's style, and anything it sets itself takes priority. (Only text components are read; other kinds are skipped.)
     */
    private static void collect(Component component, Style inherited, List<Segment> out) {
        Style style = inherited.merge(component.style());

        if (component instanceof TextComponent text && !text.content().isEmpty()) {
            out.add(new Segment(text.content(), style));
        }

        for (Component child : component.children()) {
            collect(child, style, out);
        }
    }

    // Return the formatting (bold, italic, ...) switched on in a style.
    private static Set<TextDecoration> decorationsOf(Style style) {
        Set<TextDecoration> active = EnumSet.noneOf(TextDecoration.class);
        for (TextDecoration decoration : TextDecoration.values()) {
            if (style.decoration(decoration) == TextDecoration.State.TRUE) {
                active.add(decoration);
            }
        }
        return active;
    }

    // Return the code for a colour: &c style for the 16 standard colours, #rrggbb for the rest.
    private static String colourCode(TextColor colour) {
        Character code = COLOUR_CODES.get(colour.value());
        return code != null ? "&" + code : colour.asHexString();
    }

    // Return the & code for a formatting decoration
    private static char decorationCode(TextDecoration decoration) {
        return switch (decoration) {
            case OBFUSCATED -> 'k';
            case BOLD -> 'l';
            case STRIKETHROUGH -> 'm';
            case UNDERLINED -> 'n';
            case ITALIC -> 'o';
        };
    }

    /**
     * Builds a line of text where each code is highlighted in its own colour or style, shows a tooltip, and copies itself to the clipboard when clicked.
     * Everything between the codes is plain white text.
     */
    public static Component buildCodeLine(String codes) {
        TextComponent.Builder line = Component.text();
        StringBuilder plain = new StringBuilder(); // plain text waiting to be added as one component
        int i = 0;

        while (i < codes.length()) {
            char c = codes.charAt(i);

            // &x code
            if (c == '&' && i + 1 < codes.length() && isLegacyCode(codes.charAt(i + 1))) {
                flush(line, plain);

                char code = Character.toLowerCase(codes.charAt(i + 1));
                TextColor colour = legacyCodeToColor(code);

                // Formatting codes (&l &m &n &o) show their effect on the token itself.
                // Unknown colours fall back to white.
                // &k and &r have no visible style of their own (obfuscated text would be unreadable) and so fall back to white.
                line.append(codeToken(codes.substring(i, i + 2),
                    colour != null ? colour : WHITE,
                    legacyCodeToDecoration(code)));
                i += 2;
                continue;
            }

            // #rrggbb code (fromHexString returns null if it isn't valid hex)
            if (c == '#' && i + HEX_LENGTH <= codes.length()) {
                String hex = codes.substring(i, i + HEX_LENGTH);
                TextColor colour = TextColor.fromHexString(hex);

                if (colour != null) {
                    flush(line, plain);
                    line.append(codeToken(hex, colour, null));
                    i += HEX_LENGTH;
                    continue;
                }
            }

            plain.append(c);
            i++;
        }

        flush(line, plain);
        return line.build();
    }

    // One highlighted and hoverable click-to-copy code.
    private static Component codeToken(String token, TextColor colour, TextDecoration decoration) {
        Component component = Component.text(token, colour)
            .clickEvent(ClickEvent.copyToClipboard(token))
            .hoverEvent(HoverEvent.showText(Component.text("Click to copy " + token + " to clipboard")));

        return decoration == null ? component : component.decorate(decoration);
    }

    // Adds any waiting plain text to the line as a single white component.
    private static void flush(TextComponent.Builder line, StringBuilder plain) {
        if (plain.isEmpty()) return;

        line.append(Component.text(plain.toString(), WHITE));
        plain.setLength(0);
    }

    private static boolean isLegacyCode(char c) {
        return LEGACY_CODES.indexOf(Character.toLowerCase(c)) >= 0;
    }

    // List of & colour codes and their formatting to switch to
    private static TextColor legacyCodeToColor(char code) {
        return switch (code) {
            case '0' -> NamedTextColor.BLACK;
            case '1' -> NamedTextColor.DARK_BLUE;
            case '2' -> NamedTextColor.DARK_GREEN;
            case '3' -> NamedTextColor.DARK_AQUA;
            case '4' -> NamedTextColor.DARK_RED;
            case '5' -> NamedTextColor.DARK_PURPLE;
            case '6' -> NamedTextColor.GOLD;
            case '7' -> NamedTextColor.GRAY;
            case '8' -> NamedTextColor.DARK_GRAY;
            case '9' -> NamedTextColor.BLUE;
            case 'a' -> NamedTextColor.GREEN;
            case 'b' -> NamedTextColor.AQUA;
            case 'c' -> NamedTextColor.RED;
            case 'd' -> NamedTextColor.LIGHT_PURPLE;
            case 'e' -> NamedTextColor.YELLOW;
            case 'f' -> NamedTextColor.WHITE;
            default -> null;
        };
    }

    // List of & formatting codes and their formatting to switch to
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
package me.unariginal.compoundraids.utils;

import kotlin.text.MatchResult;
import kotlin.text.Regex;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TextManager {
    String [] colorArray = {"blue", "red", "black", "dark_red", "aqua", "dark_aqua", "dark_blue", "yellow", "green", "dark_green", "light_purple", "dark_purple", "gold", "white", "gray", "dark_gray"};
    List<String> colorList = Arrays.stream(colorArray).toList();
    String[] styleArray = {"bold", "underline", "italic", "strikethrough", "obfuscated"};
    List<String> styleList = Arrays.stream(styleArray).toList();
    String[] clickEventsArray = {"open_url", "suggest_command", "run_command", "copy_to_clipboard"};
    List<String> clickEventList = Arrays.stream(clickEventsArray).toList();

    String currentFormattingColor = null;
    Integer currentColorHex = null;
    ArrayList<String> currentFormattingStyle = new ArrayList<>();

    String currentClickEvent = null;

    Text currentOnHover = null;
    Integer currentHoverColorHex = null;
    String currentHoverFormattingColor = null;
    ArrayList<String> currentHoverFormattingStyle = new ArrayList<>();

    public Text parseMessage(String message) {
        MutableText returnMessage = Text.literal("");
        String text = message;
        Regex regexIdentifier = Regex.Companion.fromLiteral("<([^<>]|<.*>)*>");
        MatchResult match = null;
        try {
            match = regexIdentifier.find(text, 0);
        } catch (Exception e) {
            returnMessage.append(sendPreviousMessage(text));
        }

        while (!text.isEmpty()) {
            if (match != null) {
                if (match.getValue().charAt(1) != '/') {
                    String matchValue = match.getValue().substring(1, match.getValue().length() - 1);
                    if (matchValue.contains("click")) {
                        String beforeString = Regex.Companion.fromLiteral("(.|\\n)*?" + match.getValue()).find(text, 0).getValue().replace(match.getValue(), "");
                        if (beforeString != null) {
                            returnMessage.append(sendPreviousMessage(beforeString));
                            text = text.substring(beforeString.length());
                        }
                        text = text.substring(match.getValue().length());

                        currentClickEvent = matchValue.replace("click:", "").replace("'", "");
                    } else if (matchValue.equals("reset")) {
                        String beforeString = Regex.Companion.fromLiteral("(.|\\n)*?" + match.getValue()).find(text, 0).getValue().replace(match.getValue(), "");
                        if (beforeString != null) {
                            returnMessage.append(sendPreviousMessage(beforeString));
                            text = text.substring(beforeString.length());
                        }
                        text = text.substring(match.getValue().length());

                        currentOnHover = null;
                        currentClickEvent = null;
                        currentFormattingStyle.clear();
                        currentFormattingColor = null;
                        currentColorHex = null;
                    } else if (matchValue.contains("hover:show_text:")) {
                        String beforeString = Regex.Companion.fromLiteral("(.|\\n)*?" + match.getValue()).find(text, 0).getValue().replace(match.getValue(), "");
                        if (beforeString != null) {
                            returnMessage.append(sendPreviousMessage(beforeString));
                            text = text.substring(beforeString.length());
                        }
                        text = text.substring(match.getValue().length());

                        currentOnHover = sendHoverMessage(matchValue.replace("hover:show_text:", "").replace("'", ""));
                    } else if (colorList.contains(matchValue)) {
                        String beforeString = Regex.Companion.fromLiteral("(.|\\n)*?" + match.getValue()).find(text, 0).getValue().replace(match.getValue(), "");
                        if (beforeString != null) {
                            returnMessage.append(sendPreviousMessage(beforeString));
                            text = text.substring(beforeString.length());
                        }
                        text = text.substring(match.getValue().length());

                        currentColorHex = null;
                        currentFormattingColor = matchValue;
                    } else if (matchValue.contains("color:")) {
                        String beforeString = Regex.Companion.fromLiteral("(.|\\n)*?" + match.getValue()).find(text, 0).getValue().replace(match.getValue(), "");
                        if (beforeString != null) {
                            returnMessage.append(sendPreviousMessage(beforeString));
                            text = text.substring(beforeString.length());
                        }
                        text = text.substring(match.getValue().length());

                        currentFormattingColor = null;
                        currentColorHex = Integer.parseInt(matchValue.replace("color:#",""), 16);
                    } else if (styleList.contains(matchValue)) {
                        String beforeString = Regex.Companion.fromLiteral("(.|\\n)*?" + match.getValue()).find(text, 0).getValue().replace(match.getValue(), "");
                        if (beforeString != null) {
                            returnMessage.append(sendPreviousMessage(beforeString));
                            text = text.substring(beforeString.length());
                        }
                        text = text.substring(match.getValue().length());

                        currentFormattingStyle.add(matchValue);
                    }
                } else {
                    String matchValue = match.getValue().substring( 1, match.getValue().length() - 1 ).replace("/", "");
                    if (matchValue.contains("click")) {
                        String beforeString = Regex.Companion.fromLiteral("(.|\\n)*?" + match.getValue()).find(text, 0).getValue().replace(match.getValue(), "");
                        if (beforeString != null) {
                            returnMessage.append(sendPreviousMessage(beforeString));
                            text = text.substring(beforeString.length());
                        }
                        text = text.substring(match.getValue().length());

                        currentClickEvent = null;
                    }
                    else if (matchValue.contains("hover")) {
                        String beforeString = Regex.Companion.fromLiteral("(.|\\n)*?" + match.getValue()).find(text, 0).getValue().replace(match.getValue(), "");
                        if (beforeString != null) {
                            returnMessage.append(sendPreviousMessage(beforeString));
                            text = text.substring(beforeString.length());
                        }
                        text = text.substring(match.getValue().length());

                        currentOnHover = null;
                    }
                    else if (colorList.contains(matchValue)) {
                        String beforeString = Regex.Companion.fromLiteral("(.|\\n)*?" + match.getValue()).find(text, 0).getValue().replace(match.getValue(), "");
                        if (beforeString != null) {
                            returnMessage.append(sendPreviousMessage(beforeString));
                            text = text.substring(beforeString.length());
                        }
                        text = text.substring(match.getValue().length());

                        currentFormattingColor = null;
                    }
                    else if (styleList.contains(matchValue)) {
                        String beforeString = Regex.Companion.fromLiteral("(.|\\n)*?" + match.getValue()).find(text, 0).getValue().replace(match.getValue(), "");
                        if (beforeString != null) {
                            returnMessage.append(sendPreviousMessage(beforeString));
                            text = text.substring(beforeString.length());
                        }
                        text = text.substring(match.getValue().length());

                        currentFormattingStyle.remove(matchValue);
                    }
                    else if (matchValue.contains("color")) {
                        String beforeString = Regex.Companion.fromLiteral("(.|\\n)*?" + match.getValue()).find(text, 0).getValue().replace(match.getValue(), "");
                        if (beforeString != null) {
                            returnMessage.append(sendPreviousMessage(beforeString));
                            text = text.substring(beforeString.length());
                        }
                        text = text.substring(match.getValue().length());

                        currentColorHex = null;
                    }
                }
            }

            try {
                assert match != null;
                match = match.next();
            } catch (Exception e) {
                returnMessage.append(sendPreviousMessage(text));
                return returnMessage;
            }
        }
        return returnMessage;
    }

    private MutableText sendPreviousMessage(String text) {
        MutableText tempText = Text.literal(text);
        if (currentFormattingColor != null) {
            switch (currentFormattingColor) {
                case "blue":
                    tempText.formatted(Formatting.BLUE);
                    break;
                case "black":
                    tempText.formatted(Formatting.BLACK);
                    break;
                case "dark_purple":
                    tempText.formatted(Formatting.DARK_PURPLE);
                    break;
                case "light_purple":
                    tempText.formatted(Formatting.LIGHT_PURPLE);
                    break;
                case "yellow":
                    tempText.formatted(Formatting.YELLOW);
                    break;
                case "green":
                    tempText.formatted(Formatting.GREEN);
                    break;
                case "gray":
                    tempText.formatted(Formatting.GRAY);
                    break;
                case "aqua":
                    tempText.formatted(Formatting.AQUA);
                    break;
                case "dark_aqua":
                    tempText.formatted(Formatting.DARK_AQUA);
                    break;
                case "dark_blue":
                    tempText.formatted(Formatting.DARK_BLUE);
                    break;
                case "dark_gray":
                    tempText.formatted(Formatting.DARK_GRAY);
                    break;
                case "dark_green":
                    tempText.formatted(Formatting.DARK_GREEN);
                    break;
                case "dark_red":
                    tempText.formatted(Formatting.DARK_RED);
                    break;
                case "gold":
                    tempText.formatted(Formatting.GOLD);
                    break;
                case "white":
                    tempText.formatted(Formatting.WHITE);
                    break;
                case "red":
                    tempText.formatted(Formatting.RED);
                    break;
            }
        } else if (currentColorHex != null) {
            tempText.setStyle(tempText.getStyle().withColor(currentColorHex));
        }

        if (!currentFormattingStyle.isEmpty()) {
            for (String style : currentFormattingStyle) {
                switch (style) {
                    case "underline":
                        tempText.formatted(Formatting.UNDERLINE);
                        break;
                    case "bold":
                        tempText.formatted(Formatting.BOLD);
                        break;
                    case "strikethrough":
                        tempText.formatted(Formatting.STRIKETHROUGH);
                        break;
                    case "italic":
                        tempText.formatted(Formatting.ITALIC);
                        break;
                    case "obfuscated":
                        tempText.formatted(Formatting.OBFUSCATED);
                        break;
                }
            }
        }

        if (currentOnHover != null) {
            tempText.setStyle(tempText.getStyle().withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, currentOnHover)));
        }

        if (currentClickEvent != null) {
            if (currentClickEvent.contains("suggest_command:")) {
                tempText.setStyle(tempText.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, currentClickEvent.replace("suggest_command:", ""))));
            } else if (currentClickEvent.contains("run_command:")) {
                tempText.setStyle(tempText.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, currentClickEvent.replace("run_command:", ""))));
            } else if (currentClickEvent.contains("open_url:")) {
                tempText.setStyle(tempText.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, currentClickEvent.replace("open_url:", ""))));
            } else if (currentClickEvent.contains("copy_to_clipboard:")) {
                tempText.setStyle(tempText.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, currentClickEvent.replace("copy_to_clipboard:", ""))));
            }
        }
        return tempText;
    }

    private Text sendHoverMessage(String string) {
        MutableText returnText = Text.literal("");
        currentHoverFormattingColor = null;
        currentHoverFormattingStyle = null;
        currentHoverColorHex = null;

        String text = string;

        Regex regexIdentifier = Regex.Companion.fromLiteral("<([^<>]|<.*>)*>");

        MatchResult match = null;
        try {
            match = regexIdentifier.find(text,0);
        }
        catch (Exception e) {
            returnText.append(parseHoverText(text));
            return returnText;
        }

        while (!text.equals("")) {
            if (match != null) {
                if (match.getValue().charAt(1) != '/') {
                    String matchValue = match.getValue().substring( 1, match.getValue().length() - 1);
                    if (matchValue.equals("reset")) {
                        String beforeString = Regex.Companion.fromLiteral("(.|\\n)*?" + match.getValue()).find(text, 0).getValue().replace(match.getValue(), "");
                        if (beforeString != null) {
                            returnText.append(parseHoverText(beforeString));
                            text = text.substring(beforeString.length());
                        }
                        text = text.substring(match.getValue().length());
                        currentHoverFormattingColor = null;
                        currentHoverFormattingStyle.clear();
                        currentHoverColorHex = null;
                    }
                    else if (colorList.contains(matchValue)) {
                        String beforeString = Regex.Companion.fromLiteral("(.|\\n)*?" + match.getValue()).find(text, 0).getValue().replace(match.getValue(), "");
                        if (beforeString != null) {
                            returnText.append(parseHoverText(beforeString));
                            text = text.substring(beforeString.length());
                        }
                        text = text.substring(match.getValue().length());

                        currentHoverColorHex = null;
                        currentHoverFormattingColor = matchValue;
                    }
                    else if (matchValue.contains("color:")) {
                        String beforeString = Regex.Companion.fromLiteral("(.|\\n)*?" + match.getValue()).find(text, 0).getValue().replace(match.getValue(), "");
                        if (beforeString != null) {
                            returnText.append(parseHoverText(beforeString));
                            text = text.substring(beforeString.length());
                        }
                        text = text.substring(match.getValue().length());


                        currentHoverFormattingColor = null;
                        currentHoverColorHex = Integer.parseInt(matchValue.replace("color:#", ""),16);

                    }
                    else if (styleList.contains(matchValue)) {
                        String beforeString = Regex.Companion.fromLiteral("(.|\\n)*?" + match.getValue()).find(text, 0).getValue().replace(match.getValue(), "");
                        if (beforeString != null) {
                            returnText.append(parseHoverText(beforeString));
                            text = text.substring(beforeString.length());
                        }
                        text = text.substring(match.getValue().length());

                        currentHoverFormattingStyle.add(matchValue);
                    }

                }
                else {
                    String matchValue = match.getValue().substring(1, match.getValue().length() - 1).replace("/", "");
                    if (colorList.contains(matchValue)) {
                        String beforeString = Regex.Companion.fromLiteral("(.|\\n)*?" + match.getValue()).find(text, 0).getValue().replace(match.getValue(), "");
                        if (beforeString != null) {
                            returnText.append(parseHoverText(beforeString));
                            text = text.substring(beforeString.length());
                        }
                        text = text.substring(match.getValue().length());

                        currentHoverFormattingColor = null;

                    }
                    else if (matchValue.contains("color")) {
                        String beforeString = Regex.Companion.fromLiteral("(.|\\n)*?" + match.getValue()).find(text, 0).getValue().replace(match.getValue(), "");
                        if (beforeString != null) {
                            returnText.append(parseHoverText(beforeString));
                            text = text.substring(beforeString.length());
                        }
                        text = text.substring(match.getValue().length());

                        currentHoverColorHex = null;

                    }
                    else if (styleList.contains(matchValue)) {
                        String beforeString = Regex.Companion.fromLiteral("(.|\\n)*?" + match.getValue()).find(text, 0).getValue().replace(match.getValue(), "");
                        if (beforeString != null) {
                            returnText.append(parseHoverText(beforeString));
                            text = text.substring(beforeString.length());
                        }
                        text = text.substring(match.getValue().length());

                        currentHoverFormattingStyle.remove(matchValue);
                    }
                }

            }

            try {
                assert match != null;
                match = match.next();
            } catch (Exception e) {
                returnText.append(parseHoverText(text));
                return returnText;
            }
        }

        return returnText;
    }

    private MutableText parseHoverText(String text) {
        MutableText tempText = Text.literal(text);
        if (currentHoverFormattingColor != null) {
            switch (currentHoverFormattingColor) {
                case "blue":
                    tempText.formatted(Formatting.BLUE);
                    break;
                case "black":
                    tempText.formatted(Formatting.BLACK);
                    break;
                case "dark_purple":
                    tempText.formatted(Formatting.DARK_PURPLE);
                    break;
                case "light_purple":
                    tempText.formatted(Formatting.LIGHT_PURPLE);
                    break;
                case "yellow":
                    tempText.formatted(Formatting.YELLOW);
                    break;
                case "green":
                    tempText.formatted(Formatting.GREEN);
                    break;
                case "gray":
                    tempText.formatted(Formatting.GRAY);
                    break;
                case "aqua":
                    tempText.formatted(Formatting.AQUA);
                    break;
                case "dark_aqua":
                    tempText.formatted(Formatting.DARK_AQUA);
                    break;
                case "dark_blue":
                    tempText.formatted(Formatting.DARK_BLUE);
                    break;
                case "dark_gray":
                    tempText.formatted(Formatting.DARK_GRAY);
                    break;
                case "dark_green":
                    tempText.formatted(Formatting.DARK_GREEN);
                    break;
                case "dark_red":
                    tempText.formatted(Formatting.DARK_RED);
                    break;
                case "gold":
                    tempText.formatted(Formatting.GOLD);
                    break;
                case "white":
                    tempText.formatted(Formatting.WHITE);
                    break;
                case "red":
                    tempText.formatted(Formatting.RED);
                    break;
            }
        } else if (currentHoverColorHex != null) {
            tempText.setStyle(tempText.getStyle().withColor(currentHoverColorHex));
        }

        if (!currentHoverFormattingStyle.isEmpty()) {
            for (String style : currentHoverFormattingStyle) {
                switch (style) {
                    case "underline":
                        tempText.formatted(Formatting.UNDERLINE);
                        break;
                    case "bold":
                        tempText.formatted(Formatting.BOLD);
                        break;
                    case "strikethrough":
                        tempText.formatted(Formatting.STRIKETHROUGH);
                        break;
                    case "italic":
                        tempText.formatted(Formatting.ITALIC);
                        break;
                    case "obfuscated":
                        tempText.formatted(Formatting.OBFUSCATED);
                        break;
                }
            }
        }
        return tempText;
    }
}


import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;

import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;


public class Editor extends Application {
    private Line cursorRender;
    private double mousePressedX;
    private double mousePressedY;
    private int lastTextHeight;
    private LinkedListDeque<Text> text = new LinkedListDeque<Text>();
    private ArrayList<LinkedListDeque> lines = new ArrayList<LinkedListDeque>();
    private final int INITIAL_TEXT_X = 5;
    private final int INITIAL_TEXT_Y = 0;
    private boolean enterOnFirstLine;
    private ArrayList<Text> firstLine;
    private int currentLine;
    private int endMargin;
    private int windWidth;
    private int windHeight;
    private int textCenterX;
    private int textCenterY;
    private double cursorX;
    private double cursorY;
    private boolean isSpace = false;
    private String fontName = "Verdana";
    public static final int TEXTSIZE = 20;
    private int fontSize = TEXTSIZE;
    private Group textRoot;
    private Group root;
    private String filename;
    private ArrayDeque<Pair> undoDeque;
    private ArrayDeque<Pair> redoDeque;
    final ScrollBar scrollBar = new ScrollBar();

    private double lineHeightFinder() {
        Text newText = new Text(0, 0, "a");
        newText.setFont(Font.font(fontName, fontSize));
        return newText.getLayoutBounds().getHeight();

    }

    public void storeAddedText(String s) {

        if (!(s.equals("\r"))) {

            Text newCharacter = new Text(getCursorXPosition(), textCenterY, s);
            textRoot.getChildren().add(newCharacter);
            newCharacter.setFont(Font.font(fontName, fontSize));

            lastTextHeight = (int) (newCharacter.getLayoutBounds().getHeight());



            boolean atFront = text.cursorAtFront();

            if (currentLine == 0) {
                firstLine.add(newCharacter);
            }
            text.insertAtCursor(newCharacter);

            if (enterOnFirstLine && atFront) {
                lines.set(0, text.getCursor());
                enterOnFirstLine = false;
            }
        } else {

            Text newCharacter = new Text(textCenterX, textCenterY, s);
            text.insertAtCursor(newCharacter);
        }

    }

    public void handleNewLine() {
        if (text.size() == 0) {
            enterOnFirstLine = true;
        }

        LinkedListDeque<Text> newLine = text.getCursor();
        int indexToAdd = (int) ((text.getCursorItem().getY()) / lastTextHeight);
        lines.add(indexToAdd, newLine);
        currentLine += 1;

        if (((Text) lines.get(0).getLineItem()).getY() != INITIAL_TEXT_Y) {
            enterOnFirstLine = true;
        }

        checkUpdateScrollBar();
        handleScrollSnap();
    }
    public void handleNewLineOpen() {
        if (text.size() == 0) {
            enterOnFirstLine = true;
        }
        text.moveCursorRight();

        LinkedListDeque<Text> newLine = text.getCursor();
        lines.add(newLine);
        currentLine += 1;

        if (((Text) lines.get(0).getLineItem()).getY() != INITIAL_TEXT_Y) {
            enterOnFirstLine = true;
        }

        checkUpdateScrollBar();
        handleScrollSnap();

    }

    public void openFile(String[] arguments, Group newRoot) {

        if (arguments.length != 1) {
            System.out.println("Expected usage: Editor <source filename>");
            System.exit(1);
        }
        String inputFilename = arguments[0];

        try {
            File inputFile = new File(inputFilename);

            if (!inputFile.exists()) {
                filename = inputFilename;

                return;
            }

            filename = inputFilename;


            FileReader reader = new FileReader(inputFile);

            BufferedReader bufferedReader = new BufferedReader(reader);

            int intRead = -1;

            while ((intRead = bufferedReader.read()) != -1) {

                char charRead = (char) intRead;
                String readChar = Character.toString(charRead);
                if (readChar.equals("\r")) {
                    storeAddedText(readChar);
                    handleNewLineOpen();
                    continue;
                }
                if (readChar.equals("\n")) {
                    continue;
                }
                storeAddedText(readChar);



            }

            text.resetCursor(text.startOfFile());

            renderText(root);

            bufferedReader.close();

        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println("File not found! Exception was: " + fileNotFoundException);
        } catch (IOException ioException) {
            System.out.println("Error when copying; exception was: " + ioException);
        }

    }

    public boolean inMiddleOfLine() {

        try {
            return (!text.cursorAtEnd() && !characterAfterEqualsNewline());
        } catch (NullPointerException nullPointerException) {
            return false;
        }


//        if (text.cursorAtEnd()) {
//            return false;
//        } else if (characterAfterEqualsNewline()) {
//            return false;
//        } else {
//            return true;
//        }
    }

    /* Function that handles rendering the text by
       iterating through the linked list */

    public void renderText(Group currentRoot) {

        handleBasicWordWrap();

        updateTextCenters();

        if (textCenterX > endMargin && inMiddleOfLine()) {
            LinkedListDeque oldCursorPosition = text.getCursor();
            text.resetCursor(text.endOfFile());
            handleWordWrapWithSpaces();
            text.resetCursor(oldCursorPosition);
            updateTextCenters();
        }

        handleScrollSnap();

        if (text.cursorAtFront()) {
            cursorX = INITIAL_TEXT_X;
            cursorY = INITIAL_TEXT_Y;
        } else if (text.getCursorItem().getText().equals("\r")
                || text.getCursorItem().getText().equals("\n")) {
            cursorX = INITIAL_TEXT_X;
            cursorY = text.getCursorItem().getY();
        } else if (!text.getCursorItem().getText().equals("\r")) {
            double characterWidth = text.getCursorItem().getLayoutBounds().getWidth();
            cursorX = text.getCursorItem().getX() + characterWidth;
            cursorY = text.getCursorItem().getY();
        }
        cursorRender.setStartX(cursorX);
        cursorRender.setStartY(cursorY);
        cursorRender.setEndX(cursorX);
        cursorRender.setEndY(cursorY + lastTextHeight);
    }

    public void updateCursor() {

        int cursorXPos = getCursorXPosition();
        int cursorYPos;
        if (text.cursorAtFront()) {
            cursorYPos = INITIAL_TEXT_Y;
        } else {
            cursorYPos = (int) Math.round(text.getCursorItem().getY());
        }

        cursorRender.setStartX(cursorXPos);
        cursorRender.setStartY(cursorYPos);
        cursorRender.setEndX(cursorXPos);
        cursorRender.setEndY(cursorYPos + lastTextHeight);


    }
    public void checkUpdateScrollBar() {
        if (lines.size() * lastTextHeight > windHeight) {
            scrollBar.setMin(0);
            scrollBar.setMax(windHeight);
        } else {
            scrollBar.setMin(0);
            scrollBar.setMax(0);
        }

    }

    public void handleScrollSnap() {
        updateCursor();


        double textHeight = lines.size() * lineHeightFinder();
        if (textHeight <= windHeight) {
            return;
        }
        double offset = Math.abs(textRoot.getLayoutY());

        double factor = (double) windHeight / Math.max(1, textHeight - windHeight);
        double cursorBottomPosition = cursorRender.getEndY();
        double cursorTopPositon = cursorRender.getStartY();

        if (cursorBottomPosition > offset + windHeight) {
            double newScrollPosition =  factor * (cursorBottomPosition - windHeight);
            scrollBar.setValue(newScrollPosition);
        } else if (cursorTopPositon < offset) {
            double newScrollPosition =  factor * cursorTopPositon;
            scrollBar.setValue(newScrollPosition);

        }
    }

    public void deleteAllNewlinesAfter(LinkedListDeque cursor) {
        text.resetCursor(cursor);
        while (!text.cursorAtEnd()) {
            if (text.getCursorItem().getText().equals("\n")) {
                lines.remove(text.getCursor());
                text.getCursorItem().setText(" ");

            }
            text.moveCursorRight();
        }
    }

    public boolean characterAfterEqualsNewline() {
        return (text.getAfterCursor().getText().equals("\r")
                || text.getAfterCursor().getText().equals("\n"));
    }


    public void handleWordWrapWithSpaces() {

        boolean insertionOperation = inMiddleOfLine();

        LinkedListDeque oldCursorPosition = text.getCursor();
        text.resetCursor(text.startOfFile());
        text.moveCursorRight();

        ArrayList<LinkedListDeque> lastSpacesInLine = new ArrayList<LinkedListDeque>();

        int initalY = (int) text.getCursorItem().getY();
        LinkedListDeque storedSpace = new LinkedListDeque();

        for (Text character : text) {
            int currentY = (int) text.getCursorItem().getY();

            if (text.getCursorItem().getText().equals(" ") && currentY == initalY) {
                storedSpace = text.getCursor();
            }
            if (getCursorXPosition() > endMargin && initalY == currentY) {
                lastSpacesInLine.add(storedSpace);
                if (insertionOperation) {
                    deleteAllNewlinesAfter(text.getCursor());
                }
            }

            if (currentY != initalY) {
                initalY = currentY;
            }
            text.moveCursorRight();
        }

        if (lastSpacesInLine.size() == 0) {
            text.resetCursor(oldCursorPosition);
            return;
        }

        for (LinkedListDeque spaces : lastSpacesInLine) {
            int index = (int) (initalY / lastTextHeight);
            text.resetCursor(spaces);
            text.getCursorItem().setText("\n");
            if (!insertionOperation) {
                lines.add(index + 1, text.getCursor());
            } else {
                index = (int) (text.getCursorItem().getY() / lastTextHeight);
                lines.add(index + 1, text.getCursor());
            }

            checkUpdateScrollBar();
        }
        text.resetCursor(oldCursorPosition);
        updateCursor();
        lastSpacesInLine.clear();
    }
    public void printCursor() {
        if (text.cursorAtFront()) {

            System.out.println(INITIAL_TEXT_X + ", " +  (INITIAL_TEXT_Y));

        } else {
            Text cursorPosition = text.getCursorItem();

            int yPos = (int) cursorPosition.getY();

            if (text.getCursorItem().getText().equals("\r")) {
                System.out.println(INITIAL_TEXT_X + ", " + (yPos));
                return;
            }

            int xPos = getCursorXPosition();

            System.out.println((xPos) + ", " + (yPos));
        }
    }

    public void handleBasicWordWrap() {
        updateTextCenters();

        if (isSpace) {
            handleWordWrapWithSpaces();
            return;
        }

        LinkedListDeque oldCursorPosition = text.getCursor();
        text.resetCursor(text.startOfFile());
        text.moveCursorRight();
        ArrayList<LinkedListDeque> lineBreaks = new ArrayList<LinkedListDeque>();
        for (Text character : text) {
            if (getCursorXPosition() > endMargin) {
                lineBreaks.add(text.getCursor());
                break;
            }
            text.moveCursorRight();


        }

        if (lineBreaks.size() == 0) {
            text.resetCursor(oldCursorPosition);
            return;
        }

        for (LinkedListDeque linebreak : lineBreaks) {

            text.resetCursor(linebreak);
            text.moveCursorLeft();
            Text newline = new Text("\n");
            text.insertAtCursor(newline);
            int newLineIndex = (int) (text.getBeforeCursor().getY() / lastTextHeight) + 1;
            lines.add(newLineIndex, text.getCursor());
            checkUpdateScrollBar();

        }

        lineBreaks.clear();

        text.resetCursor(oldCursorPosition);

        updateCursor();

        handleScrollSnap();



    }
    public void updateTextCenters() {
        textCenterX = INITIAL_TEXT_X;
        textCenterY = INITIAL_TEXT_Y;

        for (Object typedCharacter : text) {
            if (((Text) typedCharacter).getText().equals("\r")
                    || ((Text) typedCharacter).getText().equals("\n")) {
                textCenterY += lastTextHeight;
                textCenterX = INITIAL_TEXT_X;
            }
            ((Text) typedCharacter).setX(textCenterX);
            ((Text) typedCharacter).setY(textCenterY);
            ((Text) typedCharacter).setFont(Font.font(fontName, fontSize));
            ((Text) typedCharacter).setTextOrigin(VPos.TOP);
            textCenterX += Math.round(((Text) typedCharacter).getLayoutBounds().getWidth());


        }




    }

    public int getCursorXPosition() {
        if (text.cursorAtFront()) {
            return INITIAL_TEXT_X;
        }

        if (text.getCursorItem().getText().equals("\r")) {
            return INITIAL_TEXT_X;
        }

        int centerX = (int) Math.round(text.getCursorItem().getX());
        int cursorXPos = centerX
                + (int) Math.round(text.getCursorItem().getLayoutBounds().getWidth());
        return cursorXPos;
    }

    private class KeyEventHandler implements EventHandler<KeyEvent> {



        /** The Text to display on the screen. */
        public static final int TEXT = 250;

        private final double SPACING_FACTOR = 1;
        private Text displayText = new Text(TEXT, TEXT, "");

        private final int OFFSET_Y = 10;
        private static final int LIMIT = 100;
        private boolean isUpdatedCenter = false;
        private boolean handledWordWrap = false;


        private KeyEventHandler(final Group root, int windowWidth, int windowHeight) {
            textCenterX = INITIAL_TEXT_X;
            textCenterY = INITIAL_TEXT_Y;
            // Initialize some empty text and add it to root so that it will be displayed.
            displayText = new Text(textCenterX, textCenterY, "");
            // Always set the text origin to be VPos.TOP! Setting the origin to be VPos.TOP means
            // that when the text is assigned a y-position, that position corresponds to the
            // highest position across all letters (for example, the top of a letter like "I", as
            // opposed to the top of a letter like "e"), which makes calculating positions much
            // simpler!
            displayText.setTextOrigin(VPos.TOP);
            displayText.setFont(Font.font(fontName, fontSize));
            lastTextHeight = (int) (displayText.getLayoutBounds().getHeight());
            currentLine = 0;

            lines.add(text.getFront());
            firstLine = new ArrayList<Text>();

            cursorRender = new Line(INITIAL_TEXT_X, INITIAL_TEXT_Y, INITIAL_TEXT_X, lastTextHeight);

            enterOnFirstLine = false;
            windWidth = windowWidth;
            windHeight = windowWidth;

            endMargin = (int) (windWidth - 5 - scrollBar.getLayoutBounds().getWidth());

            undoDeque = new ArrayDeque<Pair>();
            redoDeque = new ArrayDeque<Pair>();
            // All new Nodes need to be added to the root in order to be displayed.
            textRoot.getChildren().add(cursorRender);

        }

        @Override
        public void handle(KeyEvent keyEvent) {
            if (keyEvent.getEventType() == KeyEvent.KEY_TYPED && !keyEvent.isShortcutDown()) {
                // Use the KEY_TYPED event rather than KEY_PRESSED for letter keys, because with
                // the KEY_TYPED event, javafx handles the "Shift" key and associated
                // capitalization.

                String characterTyped = keyEvent.getCharacter();
                if (characterTyped.equals(" ")) {
                    isSpace = true;
                }
                if (characterTyped.charAt(0) != 8 && !(characterTyped.equals("\r"))) {

                    storeAddedText(characterTyped);
                    renderText(root);
                    Text typedCharacter = text.getCursorItem();
                    addUndoDeque("remove", typedCharacter);
                    redoDeque.clear();
                    keyEvent.consume();


                } else if (characterTyped.equals("\r")) {
                    storeAddedText(characterTyped);
                    renderText(root);
                    Text typedCharacter = text.getCursorItem();
                    addUndoDeque("remove", typedCharacter);
                    redoDeque.clear();
                    handleNewLine();

                } else if (characterTyped.charAt(0) == 8
                        && text.size() > 0 && !text.cursorAtFront()) {
                    Text toRemove = text.getCursorItem();
                    redoDeque.clear();
                    handleBackSpace();
                    renderText(root);
                    addUndoDeque("add", toRemove);
                }


            } else if (keyEvent.getEventType() == KeyEvent.KEY_PRESSED) {
                // Arrow keys should be processed using the KEY_PRESSED event, because KEY_PRESSED
                // events have a code that we can check (KEY_TYPED events don't have an associated
                // KeyCode).

                handleKeyPressed(keyEvent);

            }
        }

        public void handleKeyPressed(KeyEvent keyEvent) {
            KeyCode code = keyEvent.getCode();
            if (code == KeyCode.UP) {
                if (!text.cursorAtFront()) {
                    handleUpArrow();
                    renderText(root);
                }
            } else if (code == KeyCode.DOWN) {
                if (lines.size() > 1) {
                    handleDownArrow();
                    renderText(root);
                }
            } else if (code == KeyCode.LEFT) {
                handleLeftArrow();
                if (text.size() > 0) {
                    renderText(root);
                }
            } else if (code == KeyCode.RIGHT) {
                handleRightArrow();
                if (text.size() > 0) {
                    renderText(root);
                }
            } else if (code == KeyCode.P) {
                if (keyEvent.isShortcutDown()) {
                    printCursor();
                }
            } else if (code == KeyCode.PLUS || code == KeyCode.EQUALS) {
                if (keyEvent.isShortcutDown()) {
                    fontSize += 4;
                    if (text.size() == 0) {
                        resetLastHeight(new Text("a"));
                        cursorRender.setEndY(lastTextHeight);
                    }
                    if (text.size() > 0) {
                        Text newSizeCharacter = new Text(0, 0, "a");
                        resetLastHeight(newSizeCharacter);
                        renderText(root);
                    }
                }
            } else if (code == KeyCode.MINUS) {
                if (keyEvent.isShortcutDown()) {
                    if (fontSize > 7) {
                        fontSize -= 4;
                    }
                    if (text.size() == 0) {
                        resetLastHeight(new Text("a"));
                        cursorRender.setEndY(lastTextHeight);
                    }
                    if (text.size() > 0) {
                        Text newSizeCharacter = new Text(0, 0, "a");
                        resetLastHeight(newSizeCharacter);
                        renderText(root);
                    }
                }
            } else if (code == KeyCode.Z) {
                if (keyEvent.isShortcutDown()) {
                    if (text.size() > 0) {
                        handleUndo();
                        renderText(root);
                    } else {
                        if (undoDeque.size() > 0) {
                            handleUndo();
                            renderText(root);
                        }
                    }
                }
            } else if (code == KeyCode.Y) {
                if (keyEvent.isShortcutDown()) {
                    if (redoDeque.size() > 0) {
                        handleRedo();
                        renderText(root);
                    }
                }
            } else if (code == KeyCode.S) {
                if (keyEvent.isShortcutDown()) {
                    handleSave();
                }
            }
        }

        public void handleSave() {
            try {
                FileWriter writer = new FileWriter(filename);

                for (Text character : text) {
                    char charRead = character.getText().charAt(0);
//                    if (charRead == '\r') {
//                        charRead = '\n';
//                    }
                    writer.write(charRead);
                }


                writer.close();
            } catch (FileNotFoundException fileNotFoundException) {
                System.out.println("File not found! Exception was: " + fileNotFoundException);
            } catch (IOException ioException) {
                System.out.println("Error when copying; exception was: " + ioException);
                System.exit(1);
            }

        }

        public void addUndoDeque(String isAdded, Text toAdd) {
            Pair key = new Pair(isAdded, toAdd);

            LinkedListDeque valueCursor = text.getCursor();
            Pair<Pair, LinkedListDeque> undoRedoPair =
                    new Pair<Pair, LinkedListDeque>(key, valueCursor);
            if (undoDeque.size() == LIMIT) {
                undoDeque.removeLast();
                undoDeque.push(undoRedoPair);
            } else {
                undoDeque.push(undoRedoPair);
            }

        }

        public void handleUndo() {

            if (undoDeque.size() == 0) {
                return;
            }
            Pair toUndo = undoDeque.pop();
            LinkedListDeque newCursorPosition = (LinkedListDeque) toUndo.getValue();
            text.resetCursor(newCursorPosition);
            Pair pair = (Pair) toUndo.getKey();
            String needtoRemove = (String) pair.getKey();
            Text character = (Text) pair.getValue();

            if (needtoRemove.equals("remove")) {
                handleBackSpace();
                needtoRemove = "add";
                addRedoDeque(needtoRemove, character);

            } else {
                storeAddedTextObject(character);
                needtoRemove = "remove";
                addRedoDeque(needtoRemove, character);



            }
        }

        public void addRedoDeque(String isOppositeAdded, Text toAdd) {
            Pair key = new Pair(isOppositeAdded, toAdd);
            LinkedListDeque valueCursor = text.getCursor();
            Pair<Pair, LinkedListDeque> undoRedoPair =
                    new Pair<Pair, LinkedListDeque>(key, valueCursor);
            redoDeque.push(undoRedoPair);

        }

        public void handleRedo() {
            Pair toRedo = redoDeque.pop();
            LinkedListDeque newCursorPosition = (LinkedListDeque) toRedo.getValue();
            text.resetCursor(newCursorPosition);
            Pair pair = (Pair) toRedo.getKey();
            String needtoRemove = (String) pair.getKey();
            Text character = (Text) pair.getValue();

            if (needtoRemove.equals("remove")) {
                handleBackSpace();
                needtoRemove = "add";
                addUndoDeque(needtoRemove, character);


            } else {
                storeAddedTextObject(character);
                needtoRemove = "remove";
                addUndoDeque(needtoRemove, character);
            }

        }



        public int getYPos() {
            int result = INITIAL_TEXT_Y - OFFSET_Y;
            for (int i = 0; i < currentLine; i++) {
                result += SPACING_FACTOR * lastTextHeight;
            }

            return result;
        }

        public void resetLastHeight(Text characterWithNewFont) {

            characterWithNewFont.setFont(Font.font(fontName, fontSize));
            lastTextHeight = (int) (characterWithNewFont.getLayoutBounds().getHeight());
        }

        public void storeAddedTextObject(Text character) {
            if (!(character.getText().equals("\r"))) {

                textRoot.getChildren().add(character);
                character.setFont(Font.font(fontName, fontSize));
                lastTextHeight = (int) (character.getLayoutBounds().getHeight());

                boolean atFront = text.cursorAtFront();

                if (currentLine == 0) {
                    firstLine.add(character);
                }

                text.insertPreviousDeleted();

                if (enterOnFirstLine && atFront) {
                    lines.set(0, text.getCursor());
                    enterOnFirstLine = false;
                }
            } else {
                text.insertPreviousDeleted();
            }

        }

        /* Function that updates the storage of text
        after removing an element using backspace */
        public void storeRemovedText() {
            Text toDelete = text.getCursorItem();

            text.deleteAtCursor();
            if (currentLine == 0) {
                firstLine.remove(toDelete);
            }

            textRoot.getChildren().remove(toDelete);
        }

        public void deleteAllNewlines() {
            for (Text character : text) {
                if (character.getText().equals("\n")) {
                    character.setText(" ");
                }
            }
        }

        public boolean equalsNewLineCharacter() {
            return (text.getCursorItem().getText().equals("\r")
                    || text.getCursorItem().getText().equals("\n"));
        }

        public void handleBackSpace() {

            if (!text.cursorAtFront()) {

                if (equalsNewLineCharacter()) {
                    int indexToRemove = (int) (text.getCursorItem().getY()) / lastTextHeight;
                    if (indexToRemove < lines.size()) {
                        lines.remove(indexToRemove);
                    }

                    currentLine -= 1;
                    checkUpdateScrollBar();

                }

                storeRemovedText();
                if (text.size() != 0) {
                    if (text.cursorAtFront() && text.getAfterCursor().getText().equals("\r")) {
                        enterOnFirstLine = true;
                    }


                }
            }

            handleScrollSnap();
        }

        public void handleLeftArrow() {
            if (!text.cursorAtFront()) {
                if (equalsNewLineCharacter()) {
                    currentLine -= 1;
                }
                text.moveCursorLeft();

            }

            handleScrollSnap();


        }



        public void handleRightArrow() {

            if (!text.cursorAtEnd()) {
                text.moveCursorRight();
                if (equalsNewLineCharacter()) {
                    currentLine += 1;
                }
            }

            handleScrollSnap();

        }

        public void handleUpArrow() {
            int cursorXPosition = getCursorXPosition();

            int cursorYPosition = (int) text.getCursorItem().getY();

            boolean beginningOfLine = equalsNewLineCharacter();

            if (cursorYPosition == INITIAL_TEXT_Y) {
                handleScrollSnap();
                return;
            }
            int indexOfPreviousLine = (int) (cursorYPosition / lastTextHeight) - 1;

            LinkedListDeque previousLine = lines.get(indexOfPreviousLine);
            text.resetCursor(previousLine);
            currentLine -= 1;
            if (indexOfPreviousLine != 0) {
                text.moveCursorRight();
            }

            if (enterOnFirstLine && indexOfPreviousLine == 0) {
                text.resetCursor(text.startOfFile());
                handleScrollSnap();
                return;
            }

            if (indexOfPreviousLine == 0 && text.getCursorItem().getY() != 0) {
                text.resetCursor(text.startOfFile());
                handleScrollSnap();
                return;
            }

            if (indexOfPreviousLine == 0 && equalsNewLineCharacter()) {
                text.resetCursor(text.startOfFile());
                handleScrollSnap();
                return;
            }


            if (equalsNewLineCharacter()) {
                text.resetCursor(previousLine);
                handleScrollSnap();
                return;
            }
            if (beginningOfLine && indexOfPreviousLine == 0) {
                text.resetCursor(previousLine);
                text.moveCursorLeft();
                handleScrollSnap();
                return;
            }

            int currentXPosition = getCursorXPosition();

            while (currentXPosition < cursorXPosition && !characterAfterEqualsNewline()) {
                text.moveCursorRight();
                currentXPosition = getCursorXPosition();
            }

            int upperLimit = currentXPosition - cursorXPosition;
            text.moveCursorLeft();
            int lowerLimit = cursorXPosition - getCursorXPosition();

            if (lowerLimit > upperLimit) {
                text.moveCursorRight();
            }

            handleScrollSnap();


        }

        public void handleDownArrow() {
            if (text.cursorAtFront()) {
                int indexOfNextLine = 1;
                LinkedListDeque nextLine = lines.get(indexOfNextLine);
                text.resetCursor(nextLine);
                handleScrollSnap();
                return;
            }
            int cursorXPosition = getCursorXPosition();

            int cursorYPosition = (int) text.getCursorItem().getY();


            int indexOfNextLine = (int) (cursorYPosition / lastTextHeight) + 1;
            if (indexOfNextLine == lines.size()) {
                handleScrollSnap();
                return;
            }

            LinkedListDeque nextLine = lines.get(indexOfNextLine);
            text.resetCursor(nextLine);
            currentLine += 1;

            if (cursorXPosition == INITIAL_TEXT_X) {
                handleScrollSnap();

                return;
            }

            text.moveCursorRight();

            if (equalsNewLineCharacter()) {
                text.resetCursor(nextLine);
                handleScrollSnap();
                return;
            }

            if (text.cursorAtEnd()) {
                handleScrollSnap();
                return;
            }



            int currentXPosition = getCursorXPosition();

            while (currentXPosition < cursorXPosition && !characterAfterEqualsNewline()) {
                text.moveCursorRight();
                currentXPosition = getCursorXPosition();
                if (text.cursorAtEnd()) {
                    break;
                }
            }

            int upperLimit = currentXPosition - cursorXPosition;
            text.moveCursorLeft();
            int lowerLimit = cursorXPosition - getCursorXPosition();

            if (lowerLimit > upperLimit) {
                text.moveCursorRight();
            }

            handleScrollSnap();



        }

        public ArrayList deleteAllAfterCursor() {

            ArrayList<Text> deletedText = new ArrayList<Text>();

            while (!(lines.get(currentLine).cursorAtEnd())) {
                Text deletedCharacter = (Text) lines.get(currentLine).deleteAfterCursor();
                deletedText.add(deletedCharacter);

            }

            return deletedText;

        }





    }

    private class RectangleBlinkEventHandler implements EventHandler<ActionEvent> {
        private int currentColorIndex = 0;
        private Color[] boxColors = {Color.BLACK, Color.WHITE};

        RectangleBlinkEventHandler() {
            // Set the color to be the first color in the list.
            changeColor();
        }

        private void changeColor() {
            cursorRender.setStroke(boxColors[currentColorIndex]);
            currentColorIndex = (currentColorIndex + 1) % boxColors.length;
        }

        @Override
        public void handle(ActionEvent event) {
            changeColor();
        }
    }

    public void makeRectangleColorChange() {
        // Create a Timeline that will call the "handle" function of RectangleBlinkEventHandler
        // every 1 second.
        final Timeline timeline = new Timeline();
        // The rectangle should continue blinking forever.
        timeline.setCycleCount(Timeline.INDEFINITE);
        RectangleBlinkEventHandler cursorChange = new RectangleBlinkEventHandler();
        KeyFrame keyFrame = new KeyFrame(Duration.seconds(0.5), cursorChange);
        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    private class MouseClickEventHandler implements EventHandler<MouseEvent> {
        /** A Text object that will be used to print the current mouse position. */
        Text positionText;

        MouseClickEventHandler(Group root) {
            // For now, since there's no mouse position yet, just create an empty Text object.
            positionText = new Text("");
            // We want the text to show up immediately above the position, so set the origin to be
            // VPos.BOTTOM (so the x-position we assign will be the position of the bottom of the
            // text).
//            positionText.setTextOrigin(VPos.BOTTOM);
//
//            // Add the positionText to root, so that it will be displayed on the screen.
//            root.getChildren().add(positionText);
        }


        @Override
        public void handle(MouseEvent mouseEvent) {
            // Because we registered this EventHandler using setOnMouseClicked, it will only called
            // with mouse events of type MouseEvent.MOUSE_CLICKED.  A mouse clicked event is
            // generated anytime the mouse is pressed and released on the same JavaFX node.
            mousePressedX = mouseEvent.getX();
            mousePressedY = mouseEvent.getY() - textRoot.getLayoutY();

            handleMouseClick();



            // Display text right above the click.
//            positionText.setText("(" + mousePressedX + ", " + mousePressedY + ")");
//            positionText.setX(mousePressedX);
//            positionText.setY(mousePressedY);
        }

        public void handleMouseClick() {

            if (text.size() == 0) {
                return;
            }
            int cursorXPosition = getOriginalCursorXPosition();
            int cursorYPosition;
            if (text.cursorAtFront()) {
                cursorYPosition = INITIAL_TEXT_Y;
            } else {
                cursorYPosition = (int) text.getCursorItem().getY();
            }
            int lineIndex = (int) (mousePressedY / lastTextHeight);
            if (lineIndex >= lines.size()) {
                lineIndex = lines.size() - 1;
            }
            LinkedListDeque cursorLine = lines.get(lineIndex);
            text.resetCursor(cursorLine);
            currentLine = lineIndex + 1;
            if (enterOnFirstLine && lineIndex == 0) {
                text.resetCursor(text.startOfFile());
                renderCursor();
                return;
            }
            if (lineIndex == 0 && mousePressedX <= INITIAL_TEXT_X + 0.5
                    * text.getCursorItem().getLayoutBounds().getWidth()) {
                text.resetCursor(text.startOfFile());
                renderCursor();
                return;
            }
            if (lineIndex == 0 && text.getCursorItem().getY() != INITIAL_TEXT_Y) {
                text.resetCursor(text.startOfFile());
                renderCursor();
                return;
            }
            if (mousePressedX <= INITIAL_TEXT_X) {
                renderCursor();
                return;
            }
            text.moveCursorRight();

            if (text.getCursorItem().getText().equals("\r")
                    || text.getCursorItem().getText().equals("\n")) {
                text.resetCursor(cursorLine);
                renderCursor();
                return;
            }
            if (text.cursorAtEnd()) {
                renderCursor();
                return;
            }
            int currentXPosition = getOriginalCursorXPosition();
            int mouseXPosition = (int) Math.round(mousePressedX);

            while (currentXPosition < mouseXPosition && !characterAfterEqualsNewline()) {
                text.moveCursorRight();
                currentXPosition = getOriginalCursorXPosition();
                if (text.cursorAtEnd()) {
                    break;
                }
            }
            int upperLimit = currentXPosition - mouseXPosition;
            text.moveCursorLeft();
            int lowerLimit = mouseXPosition - getOriginalCursorXPosition();

            if (lowerLimit > upperLimit) {
                text.moveCursorRight();
                renderCursor();
            }
            renderCursor();
        }

        public void renderCursor() {

            int cursorXPos = getOriginalCursorXPosition();
            int cursorYPos;
            if (text.cursorAtFront()) {
                cursorYPos = INITIAL_TEXT_Y;
            } else {
                cursorYPos = (int) Math.round(text.getCursorItem().getY());
            }

            cursorRender.setStartX(cursorXPos);
            cursorRender.setStartY(cursorYPos);
            cursorRender.setEndX(cursorXPos);
            cursorRender.setEndY(cursorYPos + lastTextHeight);


        }

        public int getOriginalCursorXPosition() {
            if (text.cursorAtFront()) {
                return INITIAL_TEXT_X;
            }

            if (text.getCursorItem().getText().equals("\r")) {
                return INITIAL_TEXT_X;
            }

            int centerX = (int) Math.round(text.getCursorItem().getX());
            int cursorXPos = centerX
                    + (int) Math.round(text.getCursorItem().getLayoutBounds().getWidth());
            return cursorXPos;
        }

        public boolean characterAfterEqualsNewline() {

            return (text.getAfterCursor().getText().equals("\r")
                    || text.getAfterCursor().getText().equals("\n"));
        }
    }

    @Override
    public void start(Stage primaryStage) {
        root = new Group();
        textRoot = new Group();
        root.getChildren().add(textRoot);

        final int  windowWidth = 500;
        final int windowHeight = 500;
        Scene scene = new Scene(root, windowWidth, windowHeight, Color.WHITE);

        EventHandler<KeyEvent> keyEventHandler =
                new KeyEventHandler(root, windowWidth, windowHeight);
        scene.setOnKeyTyped(keyEventHandler);
        scene.setOnKeyPressed(keyEventHandler);
        scene.setOnMouseClicked(new MouseClickEventHandler(root));

        makeRectangleColorChange();

        scrollBar.setOrientation(Orientation.VERTICAL);
        scrollBar.setPrefHeight(windowHeight);
        scrollBar.setMin(0);
        scrollBar.setMax(0);
        root.getChildren().add(scrollBar);
        double usableScreenWidth = windowWidth + 5 - scrollBar.getLayoutBounds().getWidth();
        scrollBar.setLayoutX(usableScreenWidth);
        scrollBar.setValue(0);

        scene.widthProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldScreenWidth,
                    Number newScreenWidth) {
                // Re-compute Allen's width.
                windWidth = newScreenWidth.intValue();
                endMargin = windWidth - 5;
                double usableScreenWidth = windWidth - scrollBar.getLayoutBounds().getWidth();
                scrollBar.setLayoutX(usableScreenWidth);
                renderText(root);

            }
        });
        scene.heightProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldScreenHeight,
                    Number newScreenHeight) {
                windHeight = newScreenHeight.intValue();
                scrollBar.setPrefHeight(windHeight);
                checkUpdateScrollBar();

            }
        });
        scrollBar.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(
                    ObservableValue<? extends Number> observableValue,
                    Number oldValue,
                    Number newValue) {
                double textHeight = lines.size() * lineHeightFinder();
                double comparison = Math.max(textHeight, windHeight);

                double adjustmentFactor = (double) (comparison - windHeight) / windHeight;
                double offSet = newValue.doubleValue() * adjustmentFactor;
                textRoot.setLayoutY(-offSet);

            }
        });
        // credit to Stackoverflow post
        // link: http://stackoverflow.com/questions/4042434/convert-arraylist
        // -containing-strings-to-an-array-of-strings-in-java
//        List<String> args = getParameters().getRaw();
//        String[] strings = args.stream().toArray(String[]::new);
//
//        openFile(strings, textRoot);
        primaryStage.setTitle(filename);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


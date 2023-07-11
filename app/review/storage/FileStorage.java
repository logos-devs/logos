package review.storage;

import app.review.proto.file.File;
import app.review.proto.file.FileType;
import dev.logos.grammar.AIPNameLexer;
import dev.logos.grammar.AIPNameParser;
import dev.logos.grammar.Java9BaseListener;
import dev.logos.grammar.Java9Lexer;
import dev.logos.grammar.Java9Parser;
import dev.logos.stack.service.storage.exceptions.EntityReadException;
import dev.logos.stack.service.storage.exceptions.EntityWriteException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;


class JavaListener extends Java9BaseListener {

    CommonTokenStream tokenStream;
    StringBuilder stringBuilder;
    Vocabulary vocabulary;
    String[] ruleNames;

    public JavaListener(
        CommonTokenStream tokenStream,
        Vocabulary vocabulary,
        String[] ruleNames
    ) {
        this.tokenStream = tokenStream;
        this.ruleNames = ruleNames;
        this.vocabulary = vocabulary;
        this.stringBuilder = new StringBuilder();
    }

    @Override
    public void enterEveryRule(final ParserRuleContext ctx) {
        stringBuilder.append(
            String.format(
                "<span class=\"%s\">",
                ruleNames[ctx.getRuleIndex()]));
    }

    @Override
    public void exitEveryRule(final ParserRuleContext ctx) {
        stringBuilder.append("</span>");
    }

    @Override
    public void visitTerminal(final TerminalNode node) {
        Token terminalSymbol = node.getSymbol();
        List<Token> tokens = tokenStream.getHiddenTokensToLeft(
            terminalSymbol.getTokenIndex());
        StringBuilder whiteSpace = new StringBuilder();
        if (tokens != null) {
            for (Token token : tokens) {

                whiteSpace.append(token.getText());
            }
        }

        stringBuilder.append(
            String.format(
                "%s<span class=\"terminal %s\">%s</span>",
                whiteSpace,
                vocabulary.getSymbolicName(
                    terminalSymbol.getType()),
                node.getText()));
    }

    @Override
    public String toString() {
        return stringBuilder.toString();
    }
}


public class FileStorage {

    String ROOT_PATH = "/home/trinque/src";

    public String getName(UUID id) {
        return null;
    }

    public File get(String name) throws EntityReadException {
        AIPNameParser nameParser =
            new AIPNameParser(
                new CommonTokenStream(
                    new AIPNameLexer(
                        CharStreams.fromString(name))));
        try {
            String fileDisplayName = nameParser
                .path()
                .file_name()
                .STRING()
                .toString();

            Java9Lexer lexer = new Java9Lexer(
                CharStreams.fromString(
                    new String(
                        Files.readAllBytes(
                            Paths.get(ROOT_PATH, name)))));

            CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            Java9Parser javaParser = new Java9Parser(tokenStream);
            JavaListener javaListener = new JavaListener(tokenStream, lexer.getVocabulary(), javaParser.getRuleNames());
            ParseTreeWalker.DEFAULT.walk(javaListener, javaParser.compilationUnit());

            return File.newBuilder()
                       .setName(name)
                       .setDisplayName(fileDisplayName)
                       .setType(FileType.REGULAR_FILE)
                       .setContents(javaListener.toString())
                       .build();
        } catch (IOException e) {
            e.printStackTrace();
            throw new EntityReadException();
        }
    }

    public List<File> list(String parent) throws EntityReadException {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
            Paths.get(ROOT_PATH, parent))) {

            ArrayList<File> fileList = new ArrayList<>();

            for (Path path : directoryStream) {
                String fileName = path.getFileName().toString();
                fileList.add(
                    // use hash as part of file aip name, get annotation moves for free
                    // conversation annotations expand into linked list UI with avatar circular pictures
                    // conversation annotations are immutable once referenced, unless the replier updates
                    // UI branches to show replies
                    File.newBuilder()
                        .setName(String.format("%s/%s", parent, fileName))
                        .setDisplayName(fileName)
                        .setType(Files.isDirectory(path) ? FileType.DIRECTORY : FileType.REGULAR_FILE)
                        .build());

            }

            return fileList;
        } catch (IOException e) {
            e.printStackTrace();
            throw new EntityReadException();
        }
    }

    public UUID create(File file) throws EntityWriteException {
        return null;
    }
}
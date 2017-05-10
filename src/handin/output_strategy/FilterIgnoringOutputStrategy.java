package handin.output_strategy;

import handin.ClientHandler;
import handin.text_events.MyTextEvent;
import handin.text_events.TextInsertEvent;
import handin.text_events.TextRemoveEvent;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;
import java.awt.*;

public class FilterIgnoringOutputStrategy implements OutputStrategy {
    private final JTextArea area;
    private final ClientHandler clientHandler;

    public FilterIgnoringOutputStrategy(JTextArea area, ClientHandler clientHandler) {
        this.area = area;
        this.clientHandler = clientHandler;
    }

    @Override
    public void output(MyTextEvent event) {
        try {
            AbstractDocument doc = (AbstractDocument) area.getDocument();
            if (event instanceof TextInsertEvent) {
                final TextInsertEvent tie = (TextInsertEvent) event;
                EventQueue.invokeLater(() -> {
                    synchronized (area) {
                        DocumentFilter filter = ((AbstractDocument) area.getDocument()).getDocumentFilter();
                        doc.setDocumentFilter(null);
                        area.insert(tie.getText(), event.getOffset());
                        doc.setDocumentFilter(filter);
                        clientHandler.setNumber(event.getNumber());
                    }
                });
            } else if (event instanceof TextRemoveEvent) {
                final TextRemoveEvent tre = (TextRemoveEvent) event;
                EventQueue.invokeLater(() -> {
                    synchronized (area) {
                        DocumentFilter filter = ((AbstractDocument) area.getDocument()).getDocumentFilter();
                        doc.setDocumentFilter(null);
                        area.replaceRange(null, tre.getOffset(), tre.getOffset() + tre.getLength());
                        doc.setDocumentFilter(filter);
                        clientHandler.setNumber(event.getNumber());
                    }
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

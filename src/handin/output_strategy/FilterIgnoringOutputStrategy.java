package handin.output_strategy;

import handin.DocumentEventCapturer;
import handin.text_events.MyTextEvent;
import handin.text_events.TextInsertEvent;
import handin.text_events.TextRemoveEvent;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;
import java.awt.*;

/**
 * Created by hjort on 5/3/17.
 */
public class FilterIgnoringOutputStrategy implements OutputStrategy {
    private JTextArea area;

    public FilterIgnoringOutputStrategy(JTextArea area) {
        this.area = area;
    }

    @Override
    public void output(MyTextEvent event) {
        try {
            //TODO The Eventqueeue.invokelate
            AbstractDocument doc = (AbstractDocument) area.getDocument();
            DocumentFilter filter = ((AbstractDocument) area.getDocument()).getDocumentFilter();
            if (event instanceof TextInsertEvent) {
                final TextInsertEvent tie = (TextInsertEvent) event;
                EventQueue.invokeLater(() -> {
                    doc.setDocumentFilter(null);
//                    filter.
                    area.insert(tie.getText(), event.getOffset());
                    doc.setDocumentFilter(filter);
                });
            } else if (event instanceof TextRemoveEvent) {
                final TextRemoveEvent tre = (TextRemoveEvent) event;
                EventQueue.invokeLater(() -> {
                    doc.setDocumentFilter(null);
                    area.replaceRange(null, tre.getOffset(), tre.getOffset() + tre.getLength());
                    doc.setDocumentFilter(filter);
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

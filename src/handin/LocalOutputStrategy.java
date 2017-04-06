package handin;

import javax.swing.*;
import java.awt.*;

public class LocalOutputStrategy implements OutputStrategy {

    private JTextArea area;

    public LocalOutputStrategy(JTextArea area) {
        this.area = area;
    }

    @Override
    public void output(MyTextEvent event) {
        try {
            if (event instanceof TextInsertEvent) {
                final TextInsertEvent tie = (TextInsertEvent) event;
                EventQueue.invokeLater(() -> area.insert(tie.getText(), event.getOffset()));
            } else if (event instanceof TextRemoveEvent) {
                final TextRemoveEvent tre = (TextRemoveEvent) event;
                EventQueue.invokeLater(() -> area.replaceRange(null, tre.getOffset(), tre.getOffset() + tre.getLength()));
            }
        }catch (Exception ex){
            System.err.print(ex);
        }
    }
}
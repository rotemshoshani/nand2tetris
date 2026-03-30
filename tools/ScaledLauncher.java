import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

/**
 * Launches HardwareSimulatorMain and wraps its content in a JScrollPane
 * so the full UI is accessible even in small tiled windows.
 */
public class ScaledLauncher {
    public static void main(String[] args) throws Exception {
        // Set the X11 WM class name so the window tiles properly under Hyprland
        try {
            var tk = Toolkit.getDefaultToolkit();
            Field f = tk.getClass().getDeclaredField("awtAppClassName");
            f.setAccessible(true);
            f.set(tk, "HardwareSimulatorMain");
        } catch (Exception ignored) {}

        // Launch the real app
        Class.forName("HardwareSimulatorMain")
            .getMethod("main", String[].class)
            .invoke(null, (Object) args);

        // Wait for frame to appear, then add scroll
        Timer timer = new Timer(500, null);
        final int[] attempts = {0};
        timer.addActionListener(e -> {
            attempts[0]++;
            for (Frame f : Frame.getFrames()) {
                if (f instanceof JFrame jf && f.isVisible()
                        && f.getTitle().contains("Hardware Simulator")) {
                    try {
                        addScroll(jf);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    timer.stop();
                    return;
                }
            }
            if (attempts[0] > 20) timer.stop();
        });
        timer.setRepeats(true);
        timer.start();
    }

    static void addScroll(JFrame frame) {
        Container contentPane = frame.getContentPane();

        // Calculate actual content bounds from children (null layout uses setBounds)
        int maxW = 0, maxH = 0;
        for (Component c : contentPane.getComponents()) {
            Rectangle b = c.getBounds();
            maxW = Math.max(maxW, b.x + b.width);
            maxH = Math.max(maxH, b.y + b.height);
        }
        // Add some padding
        maxW += 10;
        maxH += 10;

        // Create wrapper with null layout preserving absolute positions
        JPanel wrapper = new JPanel(null);
        final Dimension contentSize = new Dimension(maxW, maxH);
        wrapper.setPreferredSize(contentSize);

        // Move children to wrapper, preserving their bounds
        Component[] children = contentPane.getComponents();
        for (Component c : children) {
            Rectangle bounds = c.getBounds();
            contentPane.remove(c);
            wrapper.add(c);
            c.setBounds(bounds);
        }

        // Add scroll pane
        JScrollPane scroll = new JScrollPane(wrapper,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);

        frame.setContentPane(scroll);
        frame.setMinimumSize(new Dimension(100, 100));
        frame.revalidate();
        frame.repaint();
    }
}

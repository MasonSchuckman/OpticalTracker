import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Test
{
	public static void main() {
		
	}
    JFrame frame;
    DrawPanel drawPanel;

    private int oneX = 7;
    private int oneY = 7;

    boolean up = false;
    boolean down = true;
    boolean left = false;
    boolean right = true;

    
    public int[][]poses;
    public Test(int[][]pos)
    {
    	poses=pos;
        frame = new JFrame("Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        drawPanel = new DrawPanel();

        frame.getContentPane().add(BorderLayout.CENTER, drawPanel);

        frame.setResizable(false);
        frame.setSize(600, 600);
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
        //moveIt();
    }
    
    public void render() {
    	frame.repaint();
    }
    
    class DrawPanel extends JPanel
    {
        private static final long serialVersionUID = 1L;

        public void paintComponent(Graphics g)
        {            
        	int dif=300;
            g.setColor(Color.BLACK);
            g.fillRect(poses[0][0]+dif, poses[0][1]+dif, 6, 6);
            
            g.setColor(Color.BLUE);
          //  g.fillRect(poses[1][0]+dif, poses[1][1]+dif, 6, 6);
            
        }
    }

    private void moveIt()
    {
        while (true)
        {
            if (oneX >= 283)
            {
                right = false;
                left = true;
            }
            if (oneX <= 7)
            {
                right = true;
                left = false;
            }
            if (oneY >= 259)
            {
                up = true;
                down = false;
            }
            if (oneY <= 7)
            {
                up = false;
                down = true;
            }
            if (up) oneY--;
            if (down) oneY++;
            if (left) oneX--;
            if (right) oneX++;
            try
            {
                Thread.sleep(10);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            frame.repaint();
        }
    }
}
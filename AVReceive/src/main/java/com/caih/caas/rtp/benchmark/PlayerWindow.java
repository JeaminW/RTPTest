package com.caih.caas.rtp.benchmark;

import javax.media.Player;
import javax.media.rtp.ReceiveStream;
import java.awt.*;

/**
 * Created by jeaminw on 17/5/4.
 * GUI classes for the Player.
 */
public class PlayerWindow extends Frame {

    Player player;
    ReceiveStream stream;

    PlayerWindow(Player p, ReceiveStream strm) {
        player = p;
        stream = strm;
    }

    public void initialize() {
        add(new PlayerPanel(player));
    }

    public void close() {
        player.close();
        setVisible(false);
        dispose();
    }

    public void addNotify() {
        super.addNotify();
        pack();
    }

    /**
     * GUI classes for the Player.
     */
    class PlayerPanel extends Panel {

        Component vc, cc;

        PlayerPanel(Player p) {
            setLayout(new BorderLayout());
            if ((vc = p.getVisualComponent()) != null)
                add("Center", vc);
            if ((cc = p.getControlPanelComponent()) != null)
                add("South", cc);
        }

        public Dimension getPreferredSize() {
            int w = 0, h = 0;
            if (vc != null) {
                Dimension size = vc.getPreferredSize();
                w = size.width;
                h = size.height;
            }
            if (cc != null) {
                Dimension size = cc.getPreferredSize();
                if (w == 0)
                    w = size.width;
                h += size.height;
            }
            if (w < 160)
                w = 160;
            return new Dimension(w, h);
        }
    }
}

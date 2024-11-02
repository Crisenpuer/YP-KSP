package io.github.crisenpuer.tpksp;

import io.github.crisenpuer.tpksp.util.TelnetClient;

public class KosClient extends TelnetClient {

    public byte selectedCpu = -1;

    public KosClient() {
        super();
    }

    public void selectCpu(int cpuIdx) {
        if (super.isConnected()) {
            super.sendCommand("");
            try {
                Thread.sleep(50);
            } catch (Exception e) { e.printStackTrace(); }
            if (!(this.selectedCpu < 0)) {
                super.sendCommand("core:doevent(\"close terminal\").");
                try {
                    Thread.sleep(50);
                } catch (Exception e) { e.printStackTrace(); }
                super.sendCommand(4);
            }
            try {
                Thread.sleep(50);
            } catch (Exception e) { e.printStackTrace(); }
            String cpu = new StringBuilder()
                .append(cpuIdx)
                .toString();
            super.sendCommand(cpu);
            selectedCpu = (byte) cpuIdx; 
            try {
                Thread.sleep(50);
            } catch (Exception e) { e.printStackTrace(); }
            super.sendCommand("core:doevent(\"open terminal\").");
            super.sendCommand("set terminal:charheight to 20.");
            super.sendCommand("set terminal:width to 50.");
            super.sendCommand("set terminal:height to 40.");
        }
    }
}
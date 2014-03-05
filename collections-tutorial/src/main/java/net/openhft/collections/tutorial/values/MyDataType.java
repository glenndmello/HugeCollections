package net.openhft.collections.tutorial.values;

/**
 *
 */
public interface MyDataType {

    // Busy lock
    public void setRecord(int value);
    public int getRecord();
    void busyLockRecord() throws InterruptedException;
    void unlockRecord();

    // Field 1
    public void setField1(long value);
    public long getField1();

    // Field 2
    public void setField2(double value);
    public double getField2();
}

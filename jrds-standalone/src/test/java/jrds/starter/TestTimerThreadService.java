package jrds.starter;

import org.junit.Test;

public class TestTimerThreadService {
    @Test
    public void test() {
        TimerThreadService service = TimerThreadService.builder().build();
        service.printRelease();
    }
}

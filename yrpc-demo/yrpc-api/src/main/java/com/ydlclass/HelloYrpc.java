package com.ydlclass;

import com.ydlclass.annotation.TryTimes;

public interface HelloYrpc {

    @TryTimes(tryTimes = 3, intervalTime = 3000)
    String sayHi(String msg);

}

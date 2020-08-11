package com.example.bartomiej.skatetricks;

import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;

import java.util.List;


public class DataSamples {
    public List<Acceleration> accelerometerSamples;
    public List<AngularVelocity> gyroscopeSamples;

    public DataSamples(List<Acceleration> accelerometerData, List<AngularVelocity> gyroscopeData){
        this.accelerometerSamples = accelerometerData;
        this.gyroscopeSamples = gyroscopeData;
    }
}

package com.udacity.parent.security.util;

import com.udacity.parent.security.constant.AlarmStatus;
import com.udacity.parent.security.constant.ArmingStatus;
import com.udacity.parent.security.constant.SensorType;
import com.udacity.parent.image.service.FakeImageService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    SecurityRepository securityRepository;
    @Mock
    FakeImageService fakeImageService;
    private SensorPanel sensorPanel;
    private SecurityService securityService;
    private Sensor windowSensor;
    private Sensor doorSensor;
    private Sensor motionSensor;
    @BeforeEach
    void setUp() {
        securityService = new SecurityService(securityRepository, fakeImageService);
        sensorPanel = new SensorPanel(securityService);
        windowSensor = new Sensor("Window sensor", SensorType.WINDOW);
        doorSensor = new Sensor("Door sensor", SensorType.DOOR);
        motionSensor = new Sensor("Motion sensor", SensorType.MOTION);
    }

    /**
     * Case 1
     * If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void handleSensorActivated_alarmArmedWindowSensorActivated_pendingAlarmStatus(ArmingStatus armingStatus) {
        Mockito.when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        Mockito.when(securityService.getArmingStatus()).thenReturn(armingStatus);

        sensorPanel.setSensorActivity(windowSensor, true);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.PENDING_ALARM);
    }

    /**
     * Case 2
     * If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void handleSensorActivated_alarmArmedWindowSensorActivatedPendingAlarmStatus_alarmStatusOn(ArmingStatus armingStatus) {
        Mockito.when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Mockito.when(securityService.getArmingStatus()).thenReturn(armingStatus);

        sensorPanel.addSensor(doorSensor);
        sensorPanel.setSensorActivity(doorSensor, true);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }

    /**
     * Case 3
     * If pending alarm and all sensors are inactive, return to no alarm state.
     */
    @Test
    void handleSensorDeActivated_pendingAlarmStatus_noAlarmStatus() {
        Mockito.when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        sensorPanel.addSensor(motionSensor);
        sensorPanel.setSensorActivity(motionSensor, true);
        securityService.changeSensorActivationStatus(motionSensor, false);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atLeastOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.NO_ALARM);
    }

    /**
     * Case 4
     * If alarm is active, change in sensor state should not affect the alarm state.
     */
    @Test
    void handleAlarmActive_sensorStateChanges_alarmStateNotChange() {
        Mockito.when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        Mockito.when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        Mockito.when(securityRepository.getSensors()).thenReturn(Set.of(windowSensor, motionSensor));

        sensorPanel.addSensor(motionSensor);
        sensorPanel.setSensorActivity(motionSensor, true);

        sensorPanel.addSensor(windowSensor);
        sensorPanel.setSensorActivity(windowSensor, true);

        securityService.changeSensorActivationStatus(motionSensor, false);
        assertEquals(AlarmStatus.ALARM, securityRepository.getAlarmStatus());
    }

    /**
     * Case 5
     * If a sensor is activated while already active and the system is in pending state, change it to alarm state
     */
    @Test
    void handleOneSensorActivated_oneOtherSensorActivatedPendingAlarm_alarmStatusOne() {
        Mockito.when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        sensorPanel.addSensor(motionSensor);
        sensorPanel.setSensorActivity(motionSensor, true);

        sensorPanel.addSensor(windowSensor);
        sensorPanel.setSensorActivity(windowSensor, true);

        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atLeastOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }

    /**
     * Case 6
     * If a sensor is deactivated while already inactive, make no changes to the alarm state
     */
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class)
    void handleOneSensorDeActivated_sensorAlreadyInactivate_noChangeAlarmStatus(AlarmStatus alarmStatus) {
        Mockito.when(securityService.getAlarmStatus()).thenReturn(alarmStatus);

        sensorPanel.addSensor(motionSensor);
        sensorPanel.setSensorActivity(motionSensor, false);

        securityService.changeSensorActivationStatus(motionSensor, false);

        Assertions.assertEquals(securityRepository.getAlarmStatus(), alarmStatus);
    }

    /**
     * Case 7
     * If the camera image contains a cat while the system is armed-home, put the system into alarm status
     */
    @Test
    void handleCatDetected_imageContainCatArmedHome_alarmStatusOn() {
        Mockito.when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        Mockito.when(fakeImageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));

        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atLeastOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }

    /**
     * Case 8
     * If the camera image does not contain a cat, change the status to no alarm as long as the sensors are not active
     */
    @Test
    void handleCatNotDetected_sensorNotActive_alarmStatusOn() {
        Mockito.when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        Mockito.when(fakeImageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        Mockito.when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        sensorPanel.addSensor(windowSensor);
        sensorPanel.setSensorActivity(windowSensor, true);

        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atLeastOnce()).setAlarmStatus(captor.capture());
        assertEquals(AlarmStatus.ALARM, captor.getValue());

        Mockito.when(fakeImageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));

        assertEquals(AlarmStatus.ALARM, securityRepository.getAlarmStatus());
    }

    /**
     * Case 9
     * If the system is disarmed, set the status to no alarm
     */
    @Test
    void handleSystemStatus_armingStatusDisable_noAlarmStatus() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atLeastOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.NO_ALARM);
    }

    /**
     * Case 10
     * If the system is armed, reset all sensors to inactive.
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void handleSystemStatus_armingStatusDisable_allSensorsInactive(ArmingStatus armingStatus) {
        Mockito.when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        sensorPanel.addSensor(windowSensor);
        sensorPanel.setSensorActivity(windowSensor, true);
        sensorPanel.addSensor(doorSensor);
        sensorPanel.setSensorActivity(doorSensor, true);
        sensorPanel.addSensor(motionSensor);
        sensorPanel.setSensorActivity(motionSensor, true);

        securityService.setArmingStatus(armingStatus);

        Set<Sensor> sensors = securityService.getSensors();
        for (Sensor sensor: sensors) {
            assertFalse(sensor.getActive());
        }
    }

    /**
     * Case 11
     * If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
     */
    @Test
    void handleAlarmStatus_systemArmedHomeCatDetected_alarmStatusOn() {
        Mockito.when(securityService.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        Mockito.when(fakeImageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atLeastOnce()).setAlarmStatus(captor.capture());
        assertEquals(AlarmStatus.ALARM, captor.getValue());
    }
}
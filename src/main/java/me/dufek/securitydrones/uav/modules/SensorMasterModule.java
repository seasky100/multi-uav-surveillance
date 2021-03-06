package me.dufek.securitydrones.uav.modules;

import cz.cuni.amis.pogamut.usar2004.agent.module.datatypes.MessageDescriptor;
import cz.cuni.amis.pogamut.usar2004.agent.module.datatypes.SensorsContainer;
import cz.cuni.amis.pogamut.base.agent.module.SensorModule;
import cz.cuni.amis.pogamut.base.communication.worldview.IWorldView;
import cz.cuni.amis.pogamut.base.communication.worldview.event.IWorldEventListener;
import cz.cuni.amis.pogamut.usar2004.agent.USAR2004Bot;
import cz.cuni.amis.pogamut.usar2004.agent.module.datatypes.SensorType;
import cz.cuni.amis.pogamut.usar2004.agent.module.sensor.SuperSensor;
import cz.cuni.amis.pogamut.usar2004.communication.messages.usarinfomessages.SensorMessage;
import java.util.HashMap;
import java.util.List;

/**
 * Master module for gathering every sensor message server sends. They are saved
 * respectively by their type and than name. There is a listener for SEN
 * messages that updates data in SensorContainer. Note that this is equvalent
 * with SensorsContainerQueued. The difference is that the non queued version
 * throws out records as soon as new comes along. It writes them over. This is
 * needed when we do not care about precision - when we want to know what is
 * happening right now!
 *
 * @author vejmanm
 */
public class SensorMasterModule extends SensorModule<USAR2004Bot>
{
    protected SensorMessageListener sensorListener;
    //DataStructure representing the list of all availible SensorTypes, each is listed by its name.
    protected SensorsContainer sensorModules;

    /**
     * Private ctor
     *
     * @param bot USAR2004Bot variable for creating instance of each new record
     * in sensorModules(due to inheritance)
     */
    public SensorMasterModule(USAR2004Bot bot)
    {
        super(bot);
        sensorModules = new SensorsContainer();
        sensorListener = new SensorMessageListener(worldView);
    }

    /**
     * Collection of sensor data check.
     *
     * @return Returns false if either sensor collection is empty or null;
     */
    public Boolean isReady()
    {
        return (sensorModules != null && !sensorModules.isEmpty());
    }

    /**
     * Gets sensor message representatives from local hashmap. Returns null if
     * none matches or this hash map is empty.
     *
     * @param type String representing the type of sensor to return.
     * @return Returns List of specified type of Sensor module.
     */
    public List<SuperSensor> getSensorsByType(String type)
    {
        if(type == null)
        {
            return null;
        }
        return sensorModules.getSensorsByType(type.toLowerCase());
    }

    /**
     * Iterates through local hashmap values and seeks match. Returns null if
     * this hash map is empty. Note, that if <B>type</B> = UNKNOWN_SENSOR it
     * returns all unknown sensors.
     *
     * @param type SensorType representing the type of sensor to return.
     * @return Returns List of all sensors that suit input SensorType.
     */
    public List<SuperSensor> getSensorsBySensorType(SensorType type)
    {
        return sensorModules.getSensorsBySensorType(type);
    }

    /**
     * Iterates through local hashmap values and seeks match. Returns null if
     * this hash map is empty. Note, that if <B>type</B> = UNKNOWN_SENSOR it
     * returns all unknown sensors.
     *
     * @param type SensorType representing the type of sensor to return.
     * @return Returns List of all sensors that suit input SensorType.
     */
    public boolean isSensorReady(SensorType type)
    {
        return sensorModules.getSensorsBySensorType(type).size() > 0;
    }

    /**
     * Adds every object that can be casted to initial class to the output list.
     * Note that if You feed this method with SuperClass it will return all
     * available submodules.
     *
     * @param c Class representing the type of which the return list should be
     * @return Returns a list of eligible objects, that can be casted to Class c
     */
    public List<SuperSensor> getSensorsByClass(Class c)
    {
        return sensorModules.getSensorsByClass(c);
    }

    /**
     * Gets sensor message representatives from local hashmap specified by type
     * and by name. Returns null if none matches or this hash map is empty.
     *
     * @param type String representing the type of sensor to return.
     * @param name String representing the name of sensor to return.
     * @return Returns List of specified type of Sensor module.
     */
    public SuperSensor getSensorByTypeName(String type, String name)
    {
        if(type == null || name == null)
        {
            return null;
        }
        return (SuperSensor) sensorModules.getSensorByTypeName(type.toLowerCase(), name.toLowerCase());
    }

    /**
     * For each type of sensor it adds all individuals to the returnee List as a
     * couple (Type, Name)
     *
     * @return returns Map of couples (Type/Name) of non empty sensor
     * representatives.
     */
    public List<MessageDescriptor> getNonEmptyDescripton()
    {
        return sensorModules.getNonEmptyDescription();
    }

    /**
     * Asks SensorType enum if it knows SensorType represented by string
     * <B>type</B>. If it does, it also contains Class reference. This reference
     * is then instantiated and returned. If it does not, it returns instance of
     * base class SuperSensor which is represented by SensorType.UNKNOWN_SENSOR.
     *
     * @param type String representing possible valid SensorType.
     * @return Returns Class instance relevant to input String.
     */
    protected SuperSensor createNewSensor(SensorMessage message)
    {
        return ModuleInstanceProvider.getSensorInstanceByType(message.getType());
    }

    /**
     * Returns a flag that indicates if sensorUpdate was successful.
     *
     * @param message new SensorMessage object.
     * @return Return false if this message type with this name does not exist.
     * yet.
     */
    protected boolean updateSensorCollection(SensorMessage message)
    {
        if(!sensorModules.containsKey(message.getType().toLowerCase()))
        {
            return false;
        }
        if(sensorModules.get(message.getType().toLowerCase()).isEmpty())
        {
            return false;
        }
        if(!sensorModules.get(message.getType().toLowerCase()).containsKey(message.getName().toLowerCase()))
        {
            return false;
        }
        sensorModules.get(message.getType().toLowerCase()).get(message.getName().toLowerCase()).updateMessage(message);
        return true;
    }

    /**
     * Updates previous State on genuine Sensor or creates a new Record.
     *
     * @param message This ought to be SensorMessage caught by listener.
     */
    protected void fileMessage(SensorMessage message)
    {
        if(updateSensorCollection(message))
        {
            return;
        }
        if(!sensorModules.containsKey(message.getType().toLowerCase()))
        {
            sensorModules.put(message.getType().toLowerCase(), new HashMap<String, SuperSensor>());
        }
        if(sensorModules.get(message.getType().toLowerCase()).isEmpty() || !sensorModules.get(message.getType().toLowerCase()).containsKey(message.getName().toLowerCase()))
        {
            SuperSensor newSensor = createNewSensor(message);
            if(newSensor == null)
            {
                System.out.println("This sensor is not supported! " + message.getName());
                return;
            }
            String type = message.getType().toLowerCase();
            String name = message.getName().toLowerCase();
            newSensor.updateMessage(message);//fill the object
            sensorModules.get(type).put(name, newSensor);
        }
    }

    @Override
    protected void cleanUp()
    {
        super.cleanUp();
        sensorListener = null;
        sensorModules = null;
    }

    private class SensorMessageListener implements IWorldEventListener<SensorMessage>
    {
        @Override
        public void notify(SensorMessage event)
        {
            fileMessage(event);
        }

        public SensorMessageListener(IWorldView worldView)
        {
            worldView.addEventListener(SensorMessage.class, this);
        }
    }
}

package anti.projects.heistmc.stages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import anti.projects.heistmc.mission.MissionObjective;

public class HeistWorldData {
  private List<MissionObjective> objectives;
  public HeistWorldData() {
    objectives = new ArrayList<MissionObjective>();
  }
  
  public void addObjective(MissionObjective obj) {
    objectives.add(obj);
  }
  
  public List<MissionObjective> getObjectives() {
    return objectives;
  }
  
  // file format:
  // four bytes denoting length of the objectives section,
  // then that many bytes of data containing objective data
  public void save(OutputStream os) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    DataOutputStream bufInterface = new DataOutputStream(buffer);
    
    for (MissionObjective obj : objectives) {
      obj.saveData(bufInterface);
    }
    
    byte[] data = buffer.toByteArray();
    DataOutputStream fileOut = new DataOutputStream(os);
    fileOut.writeInt(data.length);
    fileOut.write(data);
    fileOut.close();
  }
  
  private static String getNextUTF(DataInputStream dis) throws IOException {
    try {
      return dis.readUTF();
    } catch (EOFException eof) {
      return null;
    }
  }
  
  public static HeistWorldData load(InputStream is) throws IOException {
    return load(is, new HeistWorldData());
  }
  
  public static HeistWorldData load(InputStream is, HeistWorldData data) throws IOException {
    DataInputStream dataIn = new DataInputStream(is);
    int objLen = dataIn.readInt();
    
    byte[] objData = new byte[objLen];
    is.read(objData);
    
    DataInputStream objDataIn = new DataInputStream(new ByteArrayInputStream(objData));
    
    String typeName;
    while ((typeName = getNextUTF(objDataIn)) != null) {
      try {
        Class<?> clzz = Class.forName(typeName);
        Constructor<?> con = clzz.getConstructor();
        MissionObjective obj = (MissionObjective)con.newInstance();
        obj.load(objDataIn);
        data.addObjective(obj);
      } catch (ClassNotFoundException e) {
        throw new IOException("Could not load HeistWorldData: Invalid objective type '" + typeName + "'");
      } catch (NoSuchMethodException e) {
        throw new IOException("Objective type '" + typeName + "' lacks a default constructor. ");
      } catch (SecurityException e) {
        throw new IOException("Unexpected security exception prevented objective initialization.");
      } catch (InstantiationException e) {
        throw new IOException("Could not load HeistWorldData: Unable to instantiate objective of type '" + typeName + "'");
      } catch (IllegalAccessException e) {
        throw new IOException("Could not load HeistWorldData: Default constructor for objective of type '" + typeName + "' is not visible");
      } catch (IllegalArgumentException e) {
        throw e;
      }
      catch (InvocationTargetException e) {
        throw new IOException("Could not load HeistWorldData: " + e.toString());
      } catch (ClassCastException cce) {
        throw new IOException("Type '" + typeName + "' is not a subclass of MissionObjective.");
      }
    }
    
    return data;
  }
  
  @Override
  public String toString() {
    return objectives.stream().map(new Function<MissionObjective, String>() {

      public String apply(MissionObjective t) {
        return t.toString();
      }
      
    }).collect(Collectors.joining(", "));
  }
}

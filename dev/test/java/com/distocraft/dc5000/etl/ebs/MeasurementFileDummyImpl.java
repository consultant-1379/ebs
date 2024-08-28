/**
 * This is only for testing purposes!
 * 
 * This file is a dummy version of MeasurementFile and it only collects 
 * the data that is added by calling the addData(String,String)
 * 
 * This collected data can then be compared afterwards in the testcase.
 */
package com.distocraft.dc5000.etl.ebs;

import com.distocraft.dc5000.repository.cache.DFormat;
import java.util.HashMap;
import java.util.Map;

import com.distocraft.dc5000.etl.parser.MeasurementFile;
import com.distocraft.dc5000.etl.parser.ParserDebugger;

/**
 * @author epetrmi
 *
 */
public class MeasurementFileDummyImpl implements MeasurementFile {

    private Map addedDataMap = new HashMap();
  /*
   * (non-Javadoc)
   * @see com.distocraft.dc5000.etl.parser.MeasurementFile#addData(java.util.Map)
   */
  public void addData(Map map) {
    // TODO Auto-generated method stub

  }

  /** Notice that this method can only store key/value-pairs, which means that
   * it may not represent the full functional MeasurementFile. (For example this test 
   * implementation cannot store more than one value with the same key.)
   * @see com.distocraft.dc5000.etl.parser.MeasurementFile#addData(java.lang.String, java.lang.String)
   */
  public void addData(String name, String value) {
    //Just put the stuff into somewhere where we can check it
    addedDataMap.put(name, value);

  }

  /* (non-Javadoc)
   * @see com.distocraft.dc5000.etl.parser.MeasurementFile#close()
   */
  public void close() throws Exception {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see com.distocraft.dc5000.etl.parser.MeasurementFile#getRowCount()
   */
  public int getRowCount() {
    // TODO Auto-generated method stub
    return 0;
  }

  /* (non-Javadoc)
   * @see com.distocraft.dc5000.etl.parser.MeasurementFile#isOpen()
   */
  public boolean isOpen() {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see com.distocraft.dc5000.etl.parser.MeasurementFile#metadataFound()
   */
  public boolean metadataFound() {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see com.distocraft.dc5000.etl.parser.MeasurementFile#saveData()
   */
  public void saveData() throws Exception {
    // TODO Auto-generated method stub

  }

  /* (non-Javadoc)
   * @see com.distocraft.dc5000.etl.parser.MeasurementFile#setData(java.util.Map)
   */
  public void setData(Map map) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean hasData() {
    return false;
  }

  /* (non-Javadoc)
   * @see com.distocraft.dc5000.etl.parser.MeasurementFile#setDebugger(com.distocraft.dc5000.etl.parser.ParserDebugger)
   */
  public void setDebugger(ParserDebugger debugger) {
    // TODO Auto-generated method stub

  }

  public Map getAddedDataMap(){
    return addedDataMap;
  }

@Override
public void setTransformed(boolean isTransformed) {
	// TODO Auto-generated method stub
	
}

  @Override
  public String getTagID() {
    return null;
  }

  @Override
public long getCounterVolume() {
	// TODO Auto-generated method stub
	return 0;
}

@Override
public String getDatetimeID() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public String getTypename() {
	// TODO Auto-generated method stub
	return null;
}

  @Override
  public DFormat getDataformat() {
    return null;
  }

  public String getTimeLevel() {
	  return null;
  }

}

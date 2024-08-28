/**
 * 
 */
package com.distocraft.dc5000.etl.ebs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import com.distocraft.dc5000.etl.parser.MeasurementFile;
import com.distocraft.dc5000.etl.parser.SourceFile;


/**
 * @author epetrmi
 *
 */
public class EBSDocInMemContentHandlerTest {

  //Constants
  private final String EMPTY = "";
  private final String CDATA = "CDATA";

  EBSDocInMemContentHandler eh;
  MeasurementFileDummyImpl dummyMeasFile;
  
  @Before
  public void prepareTesting(){
    initializeEBSHandler();
  }
  
  @After public void cleanUpTesting(){
    eh = null;
    dummyMeasFile = null;
    
  }
  
  /**
   * Test method for {@link com.distocraft.dc5000.etl.ebs.parser.EBSDocInMemContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)}.
   *
   * startElement(final String uri, final String name,
   *  final String qName, final Attributes atts)
   *
   * <p>
   * Tests the situation where parser reaches mi-start-tag in xml-file. 
   * MeasInfoId must be read from the xml correctly and also 
   * measurementName/Value maps must be initialized correctly.
   * </p>
   *
   */
  @Test 
  public void testStartElement_emptyStr_emptyStr_mi_measInfoId() throws Exception{
    //The value we put into xmlElement and which we test 
    String testValue = "someSimpleTestValue";
    
    //Set up the parameters for the startElement-method
    AttributesImpl attrImpl = new AttributesImpl();
    attrImpl.addAttribute(null, null, "measInfoId", CDATA, testValue);  
    Attributes xmlTagAttributes = attrImpl;
    
    //Actual method call
    eh.startElement(EMPTY, EMPTY, eh.TAG_MEAS_INFO, xmlTagAttributes);
    assertEquals(eh.TAG_MEAS_INFO+"-tag was not properly read and stored.", testValue, eh.getMeasInfoId());
    assertTrue("Measurement name map not properly initialized. " , isMapNotNullAndZeroSized(eh.getMeasNameMap()));
  }

  
   /**
    * Test "characters(...) -method with normal string value 
    * 
    * @throws Exception
    */
  @Test
   public void testCharacters_1() throws Exception {
     initializeEBSHandler();
     //textContent
     final String valueStr = "32.401 V6.2";
     final char[] value = valueStr.toCharArray();
     eh.characters(value, 0, value.length);
     assertEquals("textContent does not match. ", valueStr, eh.getCharValue() );
   }
  
  
   /**
   * Test "characters(...) -method with string value that contains also linebreaks/tabs 
   * 
   * @throws Exception
   */
   @Test
   public void testCharacters_2() throws Exception {
     initializeEBSHandler();
     //textContent
     final String inputStr = "\n32.401 \tV6.2";
     final String expected = "32.401 V6.2";
     final char[] value = inputStr.toCharArray();
     eh.characters(value, 0, value.length);
     assertEquals("textContent does not match. ", expected, eh.getCharValue() );
   }
   
  
   /**
   * 
   * Just test that when the parser reaches ffv-tag, it should also store related value
   * into a instance variable
   * 
   * @throws Exception
   */
  @Test 
    public void testEndElement_emptyStr_emptyStr_ffv_1() throws Exception{
       initializeEBSHandler();
       
       String testValue = "32.401 V6.2";
       
       //Means that characters(...)-method is called by parser before
       eh.setCharValue(testValue);
         
      //Actual method call
      eh.endElement(EMPTY, EMPTY, eh.TAG_FILE_FORMAT_VERSION);
      assertEquals(eh.TAG_FILE_FORMAT_VERSION+"-tag was not properly read and stored.", testValue, eh.getFileFormatVersion());
    }

  
  
   /**
   * Test method for {@link com.distocraft.dc5000.etl.ebs.parser.EBSDocInMemContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)}.
   *
   * endElement(final String uri, final String name, final String qName)
   * <p> 
   * Tests the situation where data is loaded/parsed in to 
   * the EBS-object and then we try to add it to correct measurementFile. The data
   * is added to measurement file when the mv-tag is reached in xml. 
   * </p>
   * <p>
   * So we fill 
   * the EBS-object by using setter-methods. Then we call the
   * endElement(...)-method with values that equal to the situation
   * where the parser has reached the mv-tag. After that we test that
   * endElement(...)-method has added the data correctly to measurementFile.
   * </p>
   * <p>
   * Actually we create imitate 3 mv-tag-contents.
   * (1) We fill measFileMap with values + add a key "moid1"
   * (2) We fill measFileMap with values + add a key "moid2"
   * (3) We fill measFileMap with values + add a key "moid1" (this should be combined with (1))
   * 
   */
 // @Test 
  public void testEndElement_emptyStr_emptyStr_mv() throws Exception{
    
    //##TODO## Refactor this object init to somewhere else
    //Set object state
    initializeEBSHandler();

    //GENERAL...
    eh.setFileFormatVersion("fileFormatVersionTESTVALUE");
    eh.setVendorName("vendorNameTESTVALUE");
    eh.setCollectionBeginTime("20080826113000");
    eh.setSn("NW=Network,BSC=AXE0");
    eh.setSt("OSS");
    eh.setNeun("AXE0");
    eh.setNedn("NW=Network,BSC=AXE0");
    eh.setTs("20080826114500");

    //...GENERAL
    
    //(1) MEAS_INFO...
    
    eh.setJobId("1jobIdTESTVALUE");
    eh.setMeasObjLdn("moid1");
    //Set durations
    eh.setGranularityPeriodDuration("100");
    eh.setRepPeriodDuration("100");

    //This is required for handling the values and types
    final String measIndex = "1";
    eh.setMeasIndex(measIndex);
    
    //Initialize the measNameMap that contains typenames (indexValue,mt-tag-content)
    Map<String,String> measNameMap = new HashMap<String,String>();
    measNameMap.put(measIndex, "measNameMapTESTVALUE");
    eh.setMeasNameMap(measNameMap);
    
    //MEAS_RESULTS...
    //Initialize the measValueMap that contains values (indexValue,r-tag-content)
    //Because of the dummy implementation of MeasurementFile, we cannot add more
    //than one measurement value! (DummyMeasFileImpl uses simple map to store data)
    Map<String,String> measValueMap = new HashMap<String,String>();
    measValueMap.put(measIndex, "measValueMapTESTVALUE");
    eh.setMeasValueMap(measValueMap);
    
    //...MEAS_RESULTS
    //...MEAS_INFO

    //Actual method call
  //  eh.endElement(EMPTY, EMPTY, eh.TAG_MEAS_VALUE);
    
    
    //(2) MEAS_INFO...
    eh.setMeasObjLdn("moid2");
    eh.setJobId("2jobIdTESTVALUE");
    
    //Set durations
    eh.setGranularityPeriodDuration("200");
    eh.setRepPeriodDuration("200");

    //This is required for handling the values and types
    final String measIndex2 = "2";
    eh.setMeasIndex(measIndex2);
    
    //Initialize the measNameMap that contains typenames (indexValue,mt-tag-content)
    Map<String,String> measNameMap2 = new HashMap<String,String>();
    measNameMap2.put(measIndex2, "2measNameMapTESTVALUE");
    eh.setMeasNameMap(measNameMap2);
    
    //MEAS_RESULTS...
    //Initialize the measValueMap that contains values (indexValue,r-tag-content)
    //Because of the dummy implementation of MeasurementFile, we cannot add more
    //than one measurement value! (DummyMeasFileImpl uses simple map to store data)
    Map<String,String> measValueMap2 = new HashMap<String,String>();
    measValueMap2.put(measIndex2, "2measValueMapTESTVALUE");
    eh.setMeasValueMap(measValueMap2);
    
    //...MEAS_RESULTS
    //...MEAS_INFO

    //Actual method call
    
    eh.endElement(EMPTY, EMPTY, eh.TAG_MEAS_VALUE);
    
//(3) MEAS_INFO...
    eh.setMeasObjLdn("moid1");
    eh.setJobId("3jobIdTESTVALUE");
    
    //Set durations
    eh.setGranularityPeriodDuration("300");
    eh.setRepPeriodDuration("300");

    //This is required for handling the values and types
    final String measIndex3 = "3";
    eh.setMeasIndex(measIndex3);
    
    //Initialize the measNameMap that contains typenames (indexValue,mt-tag-content)
    Map<String,String> measNameMap3 = new HashMap<String,String>();
    measNameMap3.put(measIndex3, "3measNameMapTESTVALUE");
    eh.setMeasNameMap(measNameMap3);
    
    //MEAS_RESULTS...
    //Initialize the measValueMap that contains values (indexValue,r-tag-content)
    //Because of the dummy implementation of MeasurementFile, we cannot add more
    //than one measurement value! (DummyMeasFileImpl uses simple map to store data)
    Map<String,String> measValueMap3 = new HashMap<String,String>();
    measValueMap3.put(measIndex3, "3measValueMapTESTVALUE");
    eh.setMeasValueMap(measValueMap3);
    
    //...MEAS_RESULTS
    //...MEAS_INFO

    //Actual method call
    eh.endElement(EMPTY, EMPTY, eh.TAG_MEAS_VALUE);
    
    //...finished setting object state
    
    //Now the parser has "parser" 1,2,3 mv -tags and we test is the state of object as we expect
    
//    Map<String, MeasurementFile> mfiles = eh.getFiles();
//    assertEquals("files does not match",2,mfiles.size());//There should be 2 files
    
//    //get the values from MeasurementFile
//    Map dummyMap = ((MeasurementFileDummyImpl)eh.getMeasFile())
//                      .getAddedDataMap();
//    assertEquals("File format version does not match. ","fileFormatVersionTESTVALUE" , (String)dummyMap.get(eh.KEY_FILE_FORMAT_VERSION));
//    assertEquals("Vendor name does not match. ","vendorNameTESTVALUE" , (String)dummyMap.get(eh.KEY_VENDOR_NAME));
//    assertEquals("Collection begin time (cbt) does not match. ", "20080826113000" ,(String)dummyMap.get(eh.KEY_COLLECTION_BEGIN_TIME));
//    assertEquals("SN does not match. ", "NW=Network,BSC=AXE0" ,(String)dummyMap.get(eh.KEY_SN) );
//    assertEquals("ST does not match. ", "OSS" ,(String)dummyMap.get(eh.KEY_ST));
//    assertEquals("Neun does not match. ", "AXE0" ,(String)dummyMap.get(eh.KEY_NEUN));
//    assertEquals("Nedn does not match. ", "NW=Network,BSC=AXE0" ,(String)dummyMap.get(eh.KEY_NEDN));
//    assertEquals("TS does not match. ", "20080826114500" ,(String)dummyMap.get(eh.KEY_TS));
//    
//    assertEquals("Jobid does not match. ","jobIdTESTVALUE" , (String)dummyMap.get(eh.KEY_JOB_ID));
//    assertEquals("Granularity period end time does not match. ", "900", (String)dummyMap.get(eh.KEY_END_TIME));
//    assertEquals("Rep period duration does not match. ", "900" , (String)dummyMap.get());//##TODO## Is this not stored?
        
//    //Dummy map is storing <measType,measValue> information
//    String key = measNameMap3.get(measIndex3);//Get the measType
//    assertEquals("The given dummymap key is wrong. ", "measNameMapTESTVALUE", key);
//    
//    //Get the value and see if its stored correctly to measurement file object
//    String value = (String)dummyMap.get(key);
//    assertEquals("The given dummymap value is wrong. ", "measValueMapTESTVALUE", value);
//    
  }

  @Test
  public void testHandleMoid_empty_true_styleStatic_value_valueIndex(){
    initializeEBSHandler();
    String parsedMoid = eh.handleMoid("", true, "static", "SGSN=X", "1");
    assertEquals("SGSN=X", parsedMoid);
  }
  
  @Test
  public void testHandleMoid_null_true_styleStatic_value_valueIndex(){
    initializeEBSHandler();
    String parsedMoid = eh.handleMoid(null, true, "static", "SGSN=X", "1");
    assertEquals("SGSN=X", parsedMoid);
  }
  
  @Test
  public void testHandleMoid_empty_false_styleStatic_value_valueIndex(){
    initializeEBSHandler();
    String parsedMoid = eh.handleMoid("", false, "static", "SGSN=X", "1");
    assertEquals("", parsedMoid);
  }
  
  @Test
  public void testHandleMoid_null_false_styleStatic_value_valueIndex(){
    initializeEBSHandler();
    String parsedMoid = eh.handleMoid(null, false, "static", "SGSN=X", "1");
    assertEquals("", parsedMoid);
  }
  
  @Test
  public void testHandleMoid_moidXYZ_false_styleStatic_value_valueIndex(){
    initializeEBSHandler();
    String parsedMoid = eh.handleMoid("moidXYZ", false, "static", "SGSN=X", "1");
    assertEquals("moidXYZ", parsedMoid);
  }
  
  @Test
  public void testHandleMoid_moidXYZ_true_styleStatic_value_valueIndex(){
    initializeEBSHandler();
    String parsedMoid = eh.handleMoid("moidXYZ", true, "static", "SGSN=X", "1");
    assertEquals("moidXYZ", parsedMoid);
  }
  
  @Test
  public void testHandleMoid_empty_true_styleUnknownXYZ_value_valueIndex(){
    initializeEBSHandler();
    String parsedMoid = eh.handleMoid("", true, "UnknownXYZ", "SGSN=X", "1");
    assertEquals("1", parsedMoid);
  }
  
  @Test
  public void testHandleObjectClass_SGSNeqX_vendorIdFromData_sourcefile1_objectmask(){
    initializeEBSHandler();
    String parsedOC = eh.handleObjectClass("SGSN=X", "data", "sourcefile1", "(.+)=.+");
    assertEquals("SGSN",parsedOC);
  }
  
  @Test
  public void testHandleObjectClass_SGSNeqX_vendorIdFromFile_sourcefile1_objectmask(){
    initializeEBSHandler();
    String fileName = "A20080927.1500+0200-20080927.1515+0200_SubNetwork=EBA_SGSN,ManagedElement=SGSN01_-_1.xml";
    String parsedOC = eh.handleObjectClass("SGSN=X", "file", fileName, ".+,(.+)=.+");
    assertEquals("ManagedElement",parsedOC);
  }
  
  @Test
  public void testHandleSuspectFlag_true(){
    initializeEBSHandler();
    String handledStr = eh.handleSuspectFlag("true");
    assertEquals("TRUE",handledStr);
  }
  
  @Test
  public void testHandleSuspectFlag_false(){
    initializeEBSHandler();
    String handledStr = eh.handleSuspectFlag("false");
    assertEquals("FALSE",handledStr);
  }
  
  @Test
  public void testHandleSuspectFlag_null(){
    initializeEBSHandler();
    String handledStr = eh.handleSuspectFlag(null);
    assertEquals("",handledStr);
  }
  
  @Test
  public void testHandleSuspectFlag_TrUe(){
    initializeEBSHandler();
    String handledStr = eh.handleSuspectFlag("TrUe");
    assertEquals("TRUE",handledStr);
  }
  
  @Test
  public void testHandleSuspectFlag_TRUE(){
    initializeEBSHandler();
    String handledStr = eh.handleSuspectFlag("TRUE");
    assertEquals("TRUE",handledStr);
  }
  
  /* ###############
   *  HELPER METHODS
   * ###############
   */
  
    private void initializeEBSHandler(){
      eh = new EBSDocInMemContentHandler();
      dummyMeasFile = new MeasurementFileDummyImpl();
      eh.setMeasFile( dummyMeasFile );
    }
  
    private boolean isMapNotNullAndZeroSized(Map m){
      return (m!=null && m.size()==0);
    }

}

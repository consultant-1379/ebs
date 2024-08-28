/**
 * This EBS content handler parses the input data, 
 * creates new MeasurementFile/s and adds data into those
 * files. This Handler loads measurementData into memory and
 * saves files after all mv-tags are handled.
 *  
 */
package com.distocraft.dc5000.etl.ebs;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.distocraft.dc5000.etl.parser.Main;
import com.distocraft.dc5000.etl.parser.MeasurementFile;
import com.distocraft.dc5000.etl.parser.SourceFile;

/**
 * @author epetrmi
 * 
 */
public class EBSDocInMemContentHandler extends DefaultHandler {

  // ##TODO## Verify logger name
  private Logger log =  log = Logger.getLogger("testlogger");

  // Virtual machine timezone unlikely changes during execution of JVM
  private static final String JVM_TIMEZONE = 
    (new SimpleDateFormat("Z")).format(new Date());


  private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ssZ");

  
  /**
   * Contains all the information (Map<moid, moidData)
   */
  private Map<String, Map<String,String>> documentData = new HashMap<String, Map<String, String>>();
  
  //Stores data that will be added to measurement file
  private Map<String,String> moidData = new HashMap<String,String>();
  
  
  
  /*
   * "TAGS" are String constants that represent tag-names
   * in the input-xml-files. They are used when capturing
   * start/endElements during parsing.
   */
  // General info
  public final String TAG_FILE_HEADER = "mfh";
  public final String TAG_FILE_FORMAT_VERSION = "ffv";
  public final String TAG_VENDOR_NAME = "vn";
  public final String TAG_SN = "sn";
  public final String TAG_ST = "st";
  public final String TAG_CBT = "cbt";//##TODO## Is this collectionBeginTime?
  public final String TAG_NEUN = "neun";
  public final String TAG_NEDN = "nedn";
  public final String TAG_MEAS_DATA = "md";
  public final String TAG_TS = "ts";
    
  // Measurement info/value spesific info
  public final String TAG_MEAS_INFO = "mi";
  public final String TAG_MTS = "mts";
  public final String TAG_JOB_ID = "jobid";
  public final String TAG_GRANULARITY_PERIOD_DURATION = "gp";
  public final String TAG_REP_PERIOD_DURATION = "rp";

  public final String TAG_MEAS_TYPE = "mt";
  public final String TAG_MEAS_VALUE = "mv";
  public final String TAG_MEAS_MOID = "moid";
  public final String TAG_MEAS_RESULT = "r";
  public final String TAG_SF = "sf";
  // ..."TAGS"

  /*
   * MeasurementFile mapping "KEYS" are String constants that 
   * are used as keys when parsed data is added to MeasurementFile.
   */ 
  //GENERAL INFO
  public final String KEY_FILE_FORMAT_VERSION = "fileFormatVersion";
  public final String KEY_VENDOR_NAME = "vendorName";
  public final String KEY_COLLECTION_BEGIN_TIME = "collectionBeginTime";
  public final String KEY_SN = TAG_SN;
  public final String KEY_ST = TAG_ST;
  public final String KEY_NEUN = TAG_NEUN;
  public final String KEY_NEDN = TAG_NEDN;
  public final String KEY_TS = "ts";
  //...GENERAL INFO
  
  //MEAS INFO...
  public final String KEY_MTS = "mts";
  public final String KEY_JOB_ID = "jobId";
  public final String KEY_MEAS_INFO_ID = "measInfoId";
  public final String KEY_MOID = "MOID";
  public final String KEY_OBJECT_CLASS = "objectClass";
  public final String KEY_DC_SUSPECTFLAG = "DC_SUSPECTFLAG";
  //...MEAS INFO
  
  //OTHER INFO...
  public final String KEY_DATETIME_ID = "DATETIME_ID";
  public final String KEY_FILENAME = "filename";
  public final String KEY_JVM_TIMEZONE = "JVM_TIMEZONE";
  public final String KEY_DIRNAME = "DIRNAME";
 //...OTHER INFO

  //..."KEYS"
  

  
  /**
   * charValue contains the textContent part 
   * when the xml is parsed.
   * 
   * (Ex. <sometag>textContent</sometag>)
   * 
   */
  private String charValue;

   
  // Needed when creating a new Measurement file
  private SourceFile sourceFile;
  private String techPack;
  private String setType;
  private String setName;
  private String workerName = "";
  private MeasurementFile measFile = null;


  
  /*
   * Variables in which the parsed data is 
   * stored temporarily
   */
  //GENERAL DATA
  private String fileFormatVersion;
  private String vendorName;
  private String sn = "sn";
  private String st = "st";
  private String neun = "neun";
  private String nedn = "nedn";
  private String collectionBeginTime;
  private String ts;
  // private String collectionEndTime; //received so late, that migth not be
  // used
  //...GENERAL DATA
  
  //MEAS INFO DATA...
  private String measInfoId;
  private String mts;
  private String jobId;
  private String granularityPeriodDuration;
  private String repPeriodDuration;
  private String granularityPeriodEndTime;//##TODO## Where is this?
  
  /**
   * Stores measurement type indexes and values (MEAS_TYPE) of current MEAS_INFO
   * section. Index matches with measValueMapIndex.
   */
  private Map<String, String> measNameMap;

  /**
   * Stores measurement indexes and values (MEAS_RESULT) of current MEAS_VALUE
   * section. Index matches with measNameMapIndex.
   */
  private Map<String, String> measValueMap;
  
  private String suspectFlag = "";
  private String measIndex;
  private String measValueIndex;
  private String measObjLdn;//MOID?
  private String objectClass;
  private String oldObjClass;
  private String objectMask;
  private String readVendorIDFrom;
  private boolean fillEmptyMoid = true;
  private String fillEmptyMoidStyle = "";
  private String fillEmptyMoidValue = "";
  private String fillMissingResultValueWith ;

  //...MEAS INFO DATA (or related)
  
  
  // ####################
  // Constructors
  // ####################

  public EBSDocInMemContentHandler() {
    super();

  }

  public EBSDocInMemContentHandler(final SourceFile sf, final String techPack,
      final String setType, final String setName, final String workerName) {
    super();
    
    log = Logger.getLogger("etl." + techPack + "." + setType + "." + setName
            + ".parser.EBSParser" + workerName);
    
    this.setSourceFile(sf);
    this.techPack = techPack;
    this.setType = setType;
    this.setName = setName;
    this.workerName = workerName;

    // ##TODO## Find out a better way to give these params
    
    objectMask = sf.getProperty("x3GPPParser.vendorIDMask", ".+,(.+)=.+");
    readVendorIDFrom = sf.getProperty("EBSParser.readVendorIDFrom", "measInfoId");
    fillEmptyMoid = "true".equalsIgnoreCase(sf.getProperty(
        "x3GPPParser.FillEmptyMOID", "true"));
    fillEmptyMoidStyle = sf
        .getProperty("x3GPPParser.FillEmptyMOIDStyle", "inc");
    fillEmptyMoidValue = sf.getProperty("x3GPPParser.FillEmptyMOIDValue", "0");
    
    
  }

  // ####################
  // SAX Event handlers
  // ####################

  public void startDocument() {  }

  public void endDocument() throws SAXException {

//    MeasurementFile mFile = null
    
    // close last meas file
    if (measFile != null) {
      try {
        measFile.close();
      } catch (Exception e) {
        log.log(Level.FINEST, "Worker parser failed to exception", e);
        throw new SAXException("Error closing measurement file");
      }
    }
  }


  public void startElement(final String uri, final String name,
      final String qName, final Attributes atts) throws SAXException {

    charValue = "";

    if (TAG_MEAS_INFO.equals(qName)) {
        // ##When matching MEAS_INFO element we create a map that stores
        // the MEAS_TYPE data. MEAS_TYPE data is referenced when the
        // actual MEAS_VALUE data is handled.
        this.measInfoId = atts.getValue("measInfoId");
        log.log(Level.FINEST, "mi measInfoId=" + measInfoId);
        measNameMap = new HashMap<String, String>();
//        measValueMap = new HashMap<String, String>();//This is probably not needed

	} else if (TAG_MEAS_VALUE.equals(qName)) {
		// Initialize measValueMap
		log.log(Level.FINEST, "StartMV: Initializing measValueMap");
		measValueMap = new HashMap<String,String>(measNameMap.size());
		this.objectClass = handleObjectClass(TAG_MEAS_MOID,
				this.readVendorIDFrom, this.sourceFile.getName(),
				this.objectMask);
		measFile = createNewMeasurementFile();
        
    } else if (TAG_MEAS_TYPE.equals(qName)) {
        measIndex = atts.getValue("p");
        log.log(Level.FINEST, "meastype p=" + measIndex);

    } else if (TAG_MEAS_RESULT.equals(qName)) {
        this.measValueIndex = atts.getValue("p");
        log.log(Level.FINEST, "meas result p=" + measValueIndex);
    }
  }

  


  public void endElement(final String uri, final String name, final String qName)
      throws SAXException {
     
      //GENERAL...
   if (TAG_SN.equals(qName)) {
      this.sn = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_SN+"="+sn);
      
    } else if (TAG_ST.equals(qName)) {
      this.st = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_ST+"="+st);
      
    } else if (TAG_CBT.equals(qName)) {
      this.collectionBeginTime = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_CBT+"="+collectionBeginTime);
      
    } else if (TAG_NEUN.equals(qName)) {
      this.neun = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_NEUN+"="+neun);
      
    } else if (TAG_NEDN.equals(qName)) {
      this.nedn = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_NEDN+"="+nedn);
      
    } else if (TAG_FILE_FORMAT_VERSION.equals(qName)) {
      this.fileFormatVersion = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_FILE_FORMAT_VERSION+"="+fileFormatVersion);
      
    } else if (TAG_VENDOR_NAME.equals(qName)) {
      this.vendorName = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_VENDOR_NAME+"="+vendorName);
      
    } else if (TAG_MTS.equals(qName)) {
      this.mts = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_MTS+"="+mts);
      
    } else if (TAG_TS.equals(qName)) {
      this.ts = charValue;  
      log.log(Level.FINEST, "Tag parsed: "+TAG_TS+"="+ts);
      //...GENERAL
      
    } else if (TAG_JOB_ID.equals(qName)) {
      this.jobId = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_JOB_ID+"="+jobId);

    } else if (TAG_GRANULARITY_PERIOD_DURATION.equals(qName)) {
      this.granularityPeriodDuration = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_GRANULARITY_PERIOD_DURATION+"="+granularityPeriodDuration);

    } else if (TAG_REP_PERIOD_DURATION.equals(qName)) {
      this.repPeriodDuration = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_REP_PERIOD_DURATION+"="+repPeriodDuration);

    } else if (TAG_MEAS_MOID.equals(qName)) {
      this.measObjLdn = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_MEAS_MOID+"="+measObjLdn);
      
    } else if (TAG_SF.equals(qName)) {
      this.suspectFlag = charValue;
      log.log(Level.FINEST, "Tag parsed: "+TAG_SF+"="+suspectFlag);
      
    } else if (TAG_MEAS_TYPE.equals(qName)) {
      measNameMap.put(measIndex, charValue);
      log.log(Level.FINEST, "measNameMap.put( " + measIndex + "," + charValue
          + " )");

    } else if (TAG_MEAS_VALUE.equals(qName)) {
      try {

        moidData = new HashMap<String,String>();//Initialize
        
        // ##In the end of mv-section we combine the counters and
        // the actual data and add it to measurementFile
        // ##Traverse through the counter keys
        
        final String strNullValue = fillMissingResultValueWith==null ? "null" : fillMissingResultValueWith;
        for (String measNameIndexKey : measNameMap.keySet()) {
          
          // Maps share the same key (index)
          String addedValue = measValueMap.get(measNameIndexKey);
          if (addedValue == null || "".equals(addedValue)) {

            //Replace null or empty string with "null" -value
            addedValue = strNullValue;
            log.log(Level.FINEST, "Non-existing result (measIndex,measName)"
                + "=(" + measNameIndexKey + ","
                + measNameMap.get(measNameIndexKey)
                + ") was replaced by given nullValue=" + strNullValue);
          }

          // ##Data is added here
          moidData.put(measNameMap.get(measNameIndexKey), addedValue);
          log.log(Level.FINEST, "moidData.put( "
              + measNameMap.get(measNameIndexKey) + ", " + addedValue + " )");
        }// ..End-of-keySet-FOR

        // change file when object class changes
          
          //GENERAL INFO...
          moidData.put(KEY_VENDOR_NAME, vendorName);
          log.log(Level.FINEST, "Added to measFile: "+"vendorName: " + vendorName);
          moidData.put(KEY_FILE_FORMAT_VERSION, fileFormatVersion);
          log.log(Level.FINEST, "Added to measFile: "+"fileFormatVersion: " + fileFormatVersion);
          moidData.put(KEY_COLLECTION_BEGIN_TIME, collectionBeginTime);
          log.log(Level.FINEST, "Added to measFile: "+"collectionBeginTime: " + collectionBeginTime);
          moidData.put(KEY_SN, sn );
          log.log(Level.FINEST, "Added to measFile: "+KEY_SN+": "+sn );
          moidData.put(KEY_ST, st );
          log.log(Level.FINEST, "Added to measFile: "+KEY_ST+": "+st );
          moidData.put(KEY_NEUN, neun );
          log.log(Level.FINEST, "Added to measFile: "+KEY_NEUN+": "+neun );
          moidData.put(KEY_NEDN, nedn );
          log.log(Level.FINEST, "Added to measFile: "+KEY_NEDN+": "+nedn);
          moidData.put(KEY_TS, ts );
          log.log(Level.FINEST, "Added to measFile: "+KEY_TS+": "+ts);
          //..GENERAL INFO
                    
          //MEAS INFO...
          moidData.put(KEY_MTS, mts);
          log.log(Level.FINEST, "Added to measFile: "+"mts: " + mts);
          moidData.put(KEY_JOB_ID, jobId);
          log.log(Level.FINEST, "Added to measFile: "+"jobId: " + jobId);
          moidData.put("PERIOD_DURATION", granularityPeriodDuration);
          log
              .log(Level.FINEST, "Added to measFile: "+"PERIOD_DURATION: "
                  + granularityPeriodDuration);
          moidData.put("repPeriodDuration", repPeriodDuration);
          log.log(Level.FINEST, "Added to measFile: "+"repPeriodDuration: " + repPeriodDuration);
          moidData.put(KEY_MEAS_INFO_ID, measInfoId);
          log.log(Level.FINEST, "Added to measFile: "+"measInfoId: " + measInfoId);
          
          //Parse moid-tag
          measObjLdn = handleMoid(measObjLdn, this.fillEmptyMoid, this.fillEmptyMoidStyle, this.fillEmptyMoidValue, "1"/*##TODO##*/);
          
          moidData.put(KEY_MOID, measObjLdn);
          log.log(Level.FINEST, "Added to measFile: "+"MOID: " + measObjLdn);
		  objectClass = handleObjectClass(measInfoId,this.readVendorIDFrom, this.sourceFile.getName(),this.objectMask);
          moidData.put(KEY_OBJECT_CLASS, objectClass);
          log.log(Level.FINEST, "Added to measFile: "+"objectClass: " + objectClass);
          
          suspectFlag = handleSuspectFlag(suspectFlag);
          moidData.put(KEY_DC_SUSPECTFLAG, suspectFlag);
          log.log(Level.FINEST, "Added to measFile: "+"DC_SUSPECTFLAG: " + suspectFlag);
          
          //...MEAS INFO
          
          //OTHER INFO...
          // DATETIME_ID calculated from end time
          //String begin = calculateBegintime();
          //if (begin != null) {
            moidData.put(KEY_DATETIME_ID, collectionBeginTime);
            log.log(Level.FINEST, "Added to measFile: "+"DATETIME_ID: " + collectionBeginTime);
          //}
          
          moidData.put(KEY_FILENAME, (sourceFile == null ? "dummyfile"
              : sourceFile.getName()));
          log.log(Level.FINEST, "Added to measFile: "+"filename: "
              + (sourceFile == null ? "dummyfile" : sourceFile.getName()));
          moidData.put(KEY_JVM_TIMEZONE, JVM_TIMEZONE);
          log.log(Level.FINEST, "Added to measFile: "+"JVM_TIMEZONE: " + JVM_TIMEZONE);
          moidData.put(KEY_DIRNAME, (sourceFile == null ? "dummydir"
              : sourceFile.getDir()));
          log.log(Level.FINEST, "Added to measFile: "+"DIRNAME: "
              + (sourceFile == null ? "dummydir" : sourceFile.getDir()));
          //...OTHER INFO
          
          //addMoidDataToDocumentData(measObjLdn, moidData, documentData);
			measFile.setData(moidData);
			measFile.saveData();
          

      } catch (Exception e) {
        log.log(Level.FINEST, "Error saving measurement data", e);
        e.printStackTrace();
        throw new SAXException("Error saving measurement data: "
            + e.getMessage(), e);
      }
   
    } else if (TAG_MEAS_RESULT.equals(qName)) {
        // ##Get the actual value (text content)
        String origValue = charValue;
        
//        //##TODO## Is this a valid check!?
//        if (origValue != null && origValue.equalsIgnoreCase("NIL")) {
//          origValue = null;//##TODO## Should this be put as "null" -string instead?
//        }
        // ##Add value to measurement value map
        measValueMap.put(measValueIndex, origValue);
    }
  }

    
 /**
 * Returns TRUE or FALSE if given parameter is true or false
 * (ignore case). Otherwise returns emptystring
 * 
 * @param suspectFlag2
 * @return TRUE, FALSE or "" (emptystring)
 */
String handleSuspectFlag(String suspectFlag2) {
   String ret = ""; 
   if(suspectFlag2!=null){
      if("false".equalsIgnoreCase(suspectFlag2)){
        ret = "FALSE";
      }else if("true".equalsIgnoreCase(suspectFlag2)){
        ret = "TRUE";
      }
    }
    return ret;
  }

/**
  * Adds moidData to documentData. If moidData with same moid exist, the current data is added to existing.
  * If data does not exist with given moid, a new object is created and added to documentData.
  * 
 * @param moid
 * @param moidData
 * @param documentData
 */
private void addMoidDataToDocumentData(String moid,
		Map<String, String> moidData,
		Map<String, Map<String, String>> documentData) {

	if (moid != null && !"".equals(moid) && moidData != null) {

		// Initialize files-map
		if (documentData == null) {
			documentData = new HashMap<String, Map<String, String>>();
			log.finest("Created files-map that contains all measurement files with moid as a key");
		}

		if (documentData.containsKey(moid)) {
			// Theres data existing with the same moid so we have to combine
			// files
			Map<String, String> existingMoidData = documentData.get(moid);
			existingMoidData.putAll(moidData);
			log.finest("mapData (size=" + moidData.size()
					+ ") added to existing data");
		} else {
			// File with given moid does not exist so we can put this as a
			// new one
			documentData.put(moid, moidData);
			log.finest("mapData (size=" + moidData.size()
					+ ") added to NEW measurement file ("
					+ moidData.toString() + ")");
		}
	} else {
		System.out.println("moid :" + moid);
	}
}

  
  
  /* 
   * Handles the textContent -part in xml
   * 
   * (non-Javadoc)
   * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
   */
  public void characters(char[] ch, int start, int length) throws SAXException {
    if(charValue==null) {
      //NOTE: null+=ch[1] (where ch[1]='2') -> "null2" -String
      charValue="";
    }
    for (int i = start; i < start + length; i++) {
      // If no control char
      if (ch[i] != '\\' && ch[i] != '\n' && ch[i] != '\r' && ch[i] != '\t') {
        charValue += ch[i];
      }
    }
    log.log(Level.FINEST, "Current charValue="+charValue);
  }

  
	String handleMoid(String moid, boolean fillEmptyMoid,
			String fillEmptyMoidStyle, String fillEmptyMoidValue,
			String measValueIndex) {
		// if moid is empty and empty moids are filled.
		if (fillEmptyMoid && (moid == null || "".equals(moid))) {
			if (fillEmptyMoidStyle.equalsIgnoreCase("static")) {
				moid = fillEmptyMoidValue;
				log.finest("Filling empty moid with static value="
						+ fillEmptyMoidValue);
			} else {
				moid = measValueIndex + "";// ##TODO## Is this still valid?
				log.finest("Filling empty moid with inc value="
						+ measValueIndex);
			}
		}
		if (moid == null) {
			moid = "";// Return empty string instead of null
			log.finest("handled Moid is empty String. "
					+ "This will cause problems because we cannot store data into map with empty-String-key. "
					+ "You should set fillEmptyMoid=true, fillEmptyMoidStyle=static and fillEmptyMoidValue=foobar"
					+ "to avoid this problem.");
		}
		return moid;
	}
  String handleObjectClass(String moid, String readVendorIDFrom,
			String sourceFileName, String objectMask) {
		String objectClass = "";

		// where to read objectClass (moid)
		if ("file".equalsIgnoreCase(readVendorIDFrom)) {
			// read vendor id from file
			objectClass = parseFileName(sourceFileName, objectMask);

		} else if ("data".equalsIgnoreCase(readVendorIDFrom)) {
			// read vendor id from data
			objectClass = parseFileName(moid, objectMask);
		} else if ("measInfoId".equalsIgnoreCase(readVendorIDFrom)) {
			objectClass = parseFileName(measInfoId, objectMask);
		}// ##TODO## is empty ObjectClass really good for default behaviour???
		return objectClass;
	}

  
  /**
   * Extracts a substring from given string based on given regExp
   * 
   */
  public String parseFileName(final String str, final String regExp) {

    final Pattern pattern = Pattern.compile(regExp);
    final Matcher matcher = pattern.matcher(str);

    if (matcher.matches()) {
      final String result = matcher.group(1);
      log.finest(" regExp (" + regExp + ") found from " + str + "  :" + result);
      return result;
    } else {
      log.warning("String " + str + " doesn't match defined regExp " + regExp);
    }

    return "";

  }

  
  /**
   * Creates new measurement file-object if current measFile is null or it is not the
   * same type as the new one (oldObjClass&objectClass-check)
   * 
   * @return - reference to valid measurement file
   * @throws SAXException
   */
  private MeasurementFile createNewMeasurementFile() throws SAXException {
    log.finest("Trying to create new measurement file");
    try {
      if (sourceFile != null) {
        
        if (oldObjClass == null || !oldObjClass.equals(objectClass)) {
          // close old meas file
          if (measFile != null) {
            measFile.close();
            log.finest("Closed a measurement file");
          }
          // create new measurementFile
          log.finest("Trying to create new measurementFile with objectClass="+objectClass);
          measFile = Main.createMeasurementFile(sourceFile, objectClass,
              techPack, setType, setName, workerName, log);
          oldObjClass = objectClass;
          
          log.finest("New measurement file created");
        }
      }
    } catch (Exception e) {
      log.log(Level.FINEST, "Error opening measurement data", e);
      e.printStackTrace();
      throw new SAXException("Error opening measurement data: "
          + e.getMessage(), e);
    }
    return measFile;
  }
  
  
  // ####################
  // GETTERS AND SETTERS
  // ####################

  public MeasurementFile getMeasFile() {
    return measFile;
  }

  public void setMeasFile(MeasurementFile measFile) {
    this.measFile = measFile;
  }

  public Logger getLog() {
    return log;
  }

  public void setLog(Logger log) {
    this.log = log;
  }

  public SimpleDateFormat getSimpleDateFormat() {
    return simpleDateFormat;
  }

  public void setSimpleDateFormat(SimpleDateFormat simpleDateFormat) {
    this.simpleDateFormat = simpleDateFormat;
  }

  public Map<String, String> getMeasNameMap() {
    return measNameMap;
  }

  public void setMeasNameMap(Map<String, String> measNameMap) {
    this.measNameMap = measNameMap;
  }

  public Map<String, String> getMeasValueMap() {
    return measValueMap;
  }

  public void setMeasValueMap(Map<String, String> measValueMap) {
    this.measValueMap = measValueMap;
  }

  public String getFileFormatVersion() {
    return fileFormatVersion;
  }

  public void setFileFormatVersion(String fileFormatVersion) {
    this.fileFormatVersion = fileFormatVersion;
  }

  public String getVendorName() {
    return vendorName;
  }

  public void setVendorName(String vendorName) {
    this.vendorName = vendorName;
  }

  public String getCollectionBeginTime() {
    return collectionBeginTime;
  }

  public void setCollectionBeginTime(String collectionBeginTime) {
    this.collectionBeginTime = collectionBeginTime;
  }

  public String getGranularityPeriodDuration() {
    return granularityPeriodDuration;
  }

  public void setGranularityPeriodDuration(String granularityPeriodDuration) {
    this.granularityPeriodDuration = granularityPeriodDuration;
  }

  public String getGranularityPeriodEndTime() {
    return granularityPeriodEndTime;
  }

  public void setGranularityPeriodEndTime(String granularityPeriodEndTime) {
    this.granularityPeriodEndTime = granularityPeriodEndTime;
  }

  public String getRepPeriodDuration() {
    return repPeriodDuration;
  }

  public void setRepPeriodDuration(String repPeriodDuration) {
    this.repPeriodDuration = repPeriodDuration;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public String getMeasInfoId() {
    return measInfoId;
  }

  public void setMeasInfoId(String measInfoId) {
    this.measInfoId = measInfoId;
  }

  public String getSuspectFlag() {
    return suspectFlag;
  }

  public void setSuspectFlag(String suspectFlag) {
    this.suspectFlag = suspectFlag;
  }

  public String getMeasIndex() {
    return measIndex;
  }

  public void setMeasIndex(String measIndex) {
    this.measIndex = measIndex;
  }

  public String getMeasValueIndex() {
    return measValueIndex;
  }

  public void setMeasValueIndex(String measValueIndex) {
    this.measValueIndex = measValueIndex;
  }

  public String getMeasObjLdn() {
    return measObjLdn;
  }

  public void setMeasObjLdn(String measObjLdn) {
    this.measObjLdn = measObjLdn;
  }

  public String getCharValue() {
    return charValue;
  }

  public void setCharValue(String charValue) {
    this.charValue = charValue;
  }

  public SourceFile getSourceFile() {
    return sourceFile;
  }

  public void setSourceFile(SourceFile sourceFile) {
    this.sourceFile = sourceFile;
  }

  public String getObjectClass() {
    return objectClass;
  }

  public void setObjectClass(String objectClass) {
    this.objectClass = objectClass;
  }

  public String getOldObjClass() {
    return oldObjClass;
  }

  public void setOldObjClass(String oldObjClass) {
    this.oldObjClass = oldObjClass;
  }

  public static String getJVM_TIMEZONE() {
    return JVM_TIMEZONE;
  }

  public String getJOB_ID() {
    return TAG_JOB_ID;
  }

  public void setMts(String mts) {
    this.mts = mts;
  }

  public String getMts() {
    return mts;
  }

  public String getSn() {
    return sn;
  }

  public void setSn(String sn) {
    this.sn = sn;
  }

  public String getSt() {
    return st;
  }

  public void setSt(String st) {
    this.st = st;
  }

  public String getNeun() {
    return neun;
  }

  public void setNeun(String neun) {
    this.neun = neun;
  }

  public String getNedn() {
    return nedn;
  }

  public void setNedn(String nedn) {
    this.nedn = nedn;
  }

  public void setTs(String ts) {
    this.ts = ts;
  }

  public String getTs() {
    return ts;
  }

  public void setFillMissingResultValueWith(String fillMissingEbsResultValueWith) {
    this.fillMissingResultValueWith = fillMissingEbsResultValueWith;
  }

  public String getFillMissingResultValueWith() {
    return this.fillMissingResultValueWith;
  }

  
//  private void handleTAGmoid(String value) {
//  // TypeClassID is determined from the moid
//  // of the first mv of the md
//
//  this.objectClass = "";
//
//  // where to read objectClass (moid)
//  if ("file".equalsIgnoreCase(readVendorIDFrom)) {
//    // read vendor id from file
//    objectClass = parseFileName(sourceFile.getName(), objectMask);
//
//  } else if ("data".equalsIgnoreCase(readVendorIDFrom)) {
//
//    // if moid is empty and empty moids are filled.
//    if (fillEmptyMoid && value.length() <= 0) {
//      if (fillEmptyMoidStyle.equalsIgnoreCase("static")) {
//        value = fillEmptyMoidValue;
//      } else {
//        value = measValueIndex + "";
//      }
//    }
//
//    // read vendor id from data
//    objectClass = parseFileName(value, objectMask);
//  }
//}
  
}


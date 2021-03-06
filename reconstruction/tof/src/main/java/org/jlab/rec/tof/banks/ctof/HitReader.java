package org.jlab.rec.tof.banks.ctof;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jlab.detector.geant4.v2.CTOFGeant4Factory;
import org.jlab.detector.hits.DetHit;
import org.jlab.detector.hits.CTOFDetHit;
import org.jlab.geom.prim.Point3D;
import org.jlab.geometry.prim.Line3d;
import org.jlab.io.base.DataEvent;
import org.jlab.rec.tof.banks.BaseHit;
import org.jlab.rec.tof.banks.BaseHitReader;
import org.jlab.rec.tof.banks.IMatchedHit;
import org.jlab.rec.tof.hit.ctof.Hit;
import org.jlab.utils.groups.IndexedTable;

/**
 *
 * @author ziegler
 *
 */
public class HitReader implements IMatchedHit {

    public HitReader() {
        // TODO Auto-generated constructor stub
    }

    private List<Hit> _CTOFHits;

    public List<Hit> get_CTOFHits() {
        return _CTOFHits;
    }

    public void set_CTOFHits(List<Hit> _Hits) {
        this._CTOFHits = _Hits;
    }

    private int _numTrks;

    /**
     *
     * @param event the evio event
     * @param geometry the CTOF geometry from package
     */
    public void fetch_Hits(DataEvent event, CTOFGeant4Factory geometry,
            List<Line3d> trks, double[] paths, 
            IndexedTable constants0, 
            IndexedTable constants1, 
            IndexedTable constants2, 
            IndexedTable constants3, 
            IndexedTable constants4) {
        /*
        0: "/calibration/ctof/attenuation"),
        1: "/calibration/ctof/effective_velocity"),
        2: "/calibration/ctof/time_offsets"),
        3: "/calibration/ctof/tdc_conv"),
        4: "/calibration/ctof/status"));
        */
        _numTrks = trks.size();

        BaseHitReader hitReader = new BaseHitReader();
        IMatchedHit MH = this;
        List<BaseHit> hitList = hitReader.get_MatchedHits(event, MH);

        if (hitList.size() == 0) {
            // System.err.println("there is no FTOF bank ");
            _CTOFHits = new ArrayList<Hit>();
            return;
        }

        // Instantiates the lists of hits
        List<Hit> hits = new ArrayList<Hit>();

        int[] id = new int[hitList.size()];
        int[] paddle = new int[hitList.size()];
        int[] ADCU = new int[hitList.size()];
        int[] ADCD = new int[hitList.size()];
        int[] TDCU = new int[hitList.size()];
        int[] TDCD = new int[hitList.size()];
        int[] ADCUIdx = new int[hitList.size()];
        int[] ADCDIdx = new int[hitList.size()];
        int[] TDCUIdx = new int[hitList.size()];
        int[] TDCDIdx = new int[hitList.size()];

        for (int i = 0; i < hitList.size(); i++) {
            id[i] = (i + 1);
            paddle[i] = hitList.get(i).get_Component();
            ADCU[i] = hitList.get(i).ADC1;
            ADCD[i] = hitList.get(i).ADC2;
            TDCU[i] = hitList.get(i).TDC1;
            TDCD[i] = hitList.get(i).TDC2;
            ADCUIdx[i] = hitList.get(i).ADCbankHitIdx1;
            ADCDIdx[i] = hitList.get(i).ADCbankHitIdx2;
            TDCUIdx[i] = hitList.get(i).TDCbankHitIdx1;
            TDCDIdx[i] = hitList.get(i).TDCbankHitIdx2;

            // get the status
            //int statusU = CCDBConstants.getSTATUSU()[0][0][paddle[i] - 1];
            //int statusD = CCDBConstants.getSTATUSD()[0][0][paddle[i] - 1];
           // String statusWord = this.set_StatusWord(statusU, statusD, ADCU[i],
            //        TDCU[i], ADCD[i], TDCD[i]);

            // create the hit object
            Hit hit = new Hit(id[i], 1, 1, paddle[i], ADCU[i], TDCU[i],
                    ADCD[i], TDCD[i]);
            hit.set_ADCbankHitIdx1(ADCUIdx[i]);
            hit.set_ADCbankHitIdx2(ADCDIdx[i]);
            hit.set_TDCbankHitIdx1(TDCUIdx[i]);
            hit.set_TDCbankHitIdx2(TDCDIdx[i]);
            //hit.set_StatusWord(statusWord);
            hit.set_StatusWord(this.set_StatusWord(hit.Status1(constants4), hit.Status2(constants4), ADCU[i], TDCU[i], ADCD[i], TDCD[i]));
            hit.setPaddleLine(geometry);
            // add this hit
            hits.add(hit);
        }
        List<Hit> updated_hits = matchHitsToCVTTrk(hits, geometry, trks, paths);

        ArrayList<ArrayList<Hit>> DetHits = new ArrayList<ArrayList<Hit>>();
        for (int j = 0; j < 3; j++) {
            DetHits.add(j, new ArrayList<Hit>());
        }

        for (Hit hit : updated_hits) {
            // set the layer to get the paddle position from the geometry
            // package
            hit.set_HitParameters(1,  constants0, 
             constants1, 
             constants2, 
             constants3);
            // DetHits.get(hit.get_Panel()-1).add(hit);
        }
        // List<Hit> unique_hits = this.removeDuplicatedHits(updated_hits);

        for (Hit hit : updated_hits) {
            DetHits.get(hit.get_Panel() - 1).add(hit);
        }
        // fill the list of TOF hits
        this.set_CTOFHits(updated_hits);
    }

    public String set_StatusWord(int statusU, int statusD, int ADCU, int TDCU, int ADCD, int TDCD) {
        String statusWord = new String(); //ADCLU TDCU ADCD TDCD
        // selected ranges TDC in [0,1000], ADC in [0, 8192] requirement given by passTDC and passADC methods

        switch (statusU) {
            case 0:
                statusWord = ("" + 1 * passADC(ADCU) + "" + 1 * passTDC(TDCU) + "");// fully functioning
                break;
            case 1:
                statusWord = ("0" + "" + 1 * passTDC(TDCU) + ""); 				// no ADC
                break;
            case 2:
                statusWord = ("" + 1 * passADC(ADCU) + "" + "0"); 				// no TDC
                break;
            case 3:
                statusWord = "00";										// no TDC, no ADC
                break;
        }
        switch (statusD) {
            case 0:
                statusWord += ("" + 1 * passADC(ADCD) + "" + 1 * passTDC(TDCD) + "");// fully functioning
                break;
            case 1:
                statusWord += ("0" + "" + 1 * passTDC(TDCD) + ""); 				// no ADC
                break;
            case 2:
                statusWord += ("" + 1 * passADC(ADCD) + "" + "0"); 				// no TDC
                break;
            case 3:
                statusWord += "00";										// no TDC, no ADC
                break;

        }

        return statusWord;

    }

    private int passTDC(int tDC) {
        // selected ranges TDC in [0, ? 1000] // what is the upper limit?
        int pass = 0;
        //if(tDC>100 &&  tDC<300)
        pass = 1;
        return pass;
    }

    private int passADC(int aDC) {
        // selected ranges  ADC in [0, ? 8192]
        int pass = 0;
        //if(aDC>700 && aDC<7000)
        pass = 1;
        return pass;
    }

    private List<Hit> matchHitsToCVTTrk(List<Hit> CTOFhits, CTOFGeant4Factory ctofDetector, List<Line3d> trks, double[] paths) {
        if (trks == null || trks.size() == 0) {
            return CTOFhits; // no hits were matched with DC tracks
        }
        // Instantiates the list of hits
        List<Hit> hitList = new ArrayList<Hit>();

        for (int i = 0; i < trks.size(); i++) { // looping over the tracks find the intersection of the track with that plane
            Line3d trk = trks.get(i); //System.out.println(" trk line "+trk.toString());

            CTOFDetHit[] HitArray = new CTOFDetHit[48];
            List<DetHit> hits = ctofDetector.getIntersections(trk);

            if (hits != null && hits.size() > 0) {
                for (DetHit hit : hits) {
                    CTOFDetHit fhit = new CTOFDetHit(hit); //System.out.println(" matched hits "+fhit.toString());
                    HitArray[fhit.getPaddle() - 1] = fhit;
                }
            }
            for (Hit fhit : CTOFhits) {
                if (HitArray[fhit.get_Paddle() - 1] == null) { // there is no track matched to this hit
                    hitList.add(fhit);	// add this hit to the output list anyway
                }
            }

            for (Hit fhit : CTOFhits) {
                if (HitArray[fhit.get_Paddle() - 1] != null) {
                    CTOFDetHit matchedHit = HitArray[fhit.get_Paddle() - 1];

                    // create a new FTOF hit for each intersecting track with this hit counter 
                    // create the hit object
                    Hit hit = new Hit(fhit.get_Id(), fhit.get_Panel(), fhit.get_Sector(), fhit.get_Paddle(), fhit.get_ADC1(), fhit.get_TDC1(), fhit.get_ADC2(), fhit.get_TDC2());
                    hit.set_StatusWord(fhit.get_StatusWord());
                    hit.set_paddleLine(fhit.get_paddleLine());
                    hit.set_matchedTrackHit(matchedHit);
                    hit.set_matchedTrack(trk);
                    // get the pathlength of the track from its origin to the mid-point between the track entrance and exit from the bar
                    //double deltaPath = matchedHit.origin().distance(matchedHit.mid());
                    double deltaPath = hit.get_matchedTrack().origin().distance(matchedHit.mid());
                    
                    double pathLenTruBar = matchedHit.origin().distance(
                            matchedHit.end());
                    hit.set_TrkPathLenThruBar(pathLenTruBar);
                    hit.set_TrkPathLen(paths[i] + deltaPath);
                    // get the coordinates for the track hit, which is defined as the mid-point between its entrance and its exit from the bar
                    hit.set_TrkPosition(new Point3D(matchedHit.mid().x, matchedHit.mid().y, matchedHit.mid().z));

                    // compute the local y at the middle of the bar :
                    //----------------------------------------------
                    Point3D origPaddleLine = hit.get_paddleLine().origin();
                    Point3D trkPosinMidlBar = new Point3D(matchedHit.mid().x, matchedHit.mid().y, matchedHit.mid().z);
                    double Lov2 = hit.get_paddleLine().length() / 2;
                    double barOrigToTrkPos = origPaddleLine.distance(trkPosinMidlBar);
                    // local y:
                    hit.set_yTrk(barOrigToTrkPos - Lov2);
                    //---------------------------------------
                    hitList.add(hit);
                }
            }
        }
        return hitList;
    }

    @Override
    public String DetectorName() {
        return "CTOF";
    }

    @Override
    public List<BaseHit> MatchHits(ArrayList<BaseHit> ADCandTDCLists) {
        ArrayList<BaseHit> matchLists = new ArrayList<BaseHit>();

        if (ADCandTDCLists != null) {
            Collections.sort(ADCandTDCLists);
            // for(BaseHit h : ADCandTDCLists)
            // System.out.println(h.get_Sector()+":"+h.get_Layer()+":"+h.get_Component()+"   --   "+h.ADC1+"; "+h.ADC2+"; "+h.TDC1+"; "+h.TDC2+"; ");
            double t1 = -1;
            double t2 = -1; // t1, t2 not yet used in selection
            int adc1 = -1;
            int adc2 = -1;
            int tdc1 = -1;
            int tdc2 = -1;

            List<ArrayList<BaseHit>> hitlists = new ArrayList<ArrayList<BaseHit>>();
            for (int i = 0; i < ADCandTDCLists.size(); i++) {
                hitlists.add(new ArrayList<BaseHit>());
            }
            int index1 = 0;
            int index2 = 0;
            int index3 = 0;
            int index4 = 0;

            for (int i = 0; i < ADCandTDCLists.size(); i++) {
                BaseHit h = ADCandTDCLists.get(i);
                if (h.get_ADC1() > 0) {
                    adc1 = h.get_ADC1();
                    if (h.get_ADCTime1() > 0) {
                        t1 = h.get_ADCTime1();
                    }

                    hitlists.get(index1).add(h);
                    index1++;
                }
                if (h.get_ADC2() > 0) {
                    adc2 = h.get_ADC2();
                    if (h.get_ADCTime2() > 0) {
                        t2 = h.get_ADCTime2();
                    }

                    if (adc1 > 0 && Math.abs(adc1 - adc2) < 8000) {
                        hitlists.get(index2).add(h); // matched hit
                        index2++;
                    }
                    if (adc1 == -1) {
                        hitlists.get(index2).add(h); // not matched hit
                        index2++;
                    }
                }
                if (h.get_TDC1() > 0) {
                    tdc1 = h.get_TDC1();
                    hitlists.get(index3).add(h);
                    index3++;
                }
                if (h.get_TDC2() > 0) {
                    tdc2 = h.get_TDC2();
                    if (tdc1 > 0 && Math.abs(tdc1 - tdc2) * 24. / 1000. < 35) {
                        hitlists.get(index4).add(h);
                        index4++;
                    }
                    if (tdc1 == -1) {
                        hitlists.get(index4).add(h); // not matched hit
                        index4++;
                    }
                }
            }
            int hitNb = 0;
            for (int i = 0; i < hitlists.size(); i++) {
                if (hitlists.get(i).size() > 0) {
                    // Make the new hit
                    BaseHit hit = new BaseHit(hitlists.get(i).get(0)
                            .get_Sector(), hitlists.get(i).get(0).get_Layer(),
                            hitlists.get(i).get(0).get_Component());
                    hit.set_Id(hitNb++);
                    double t_1 = -1;
                    double t_2 = -1;
                    int ped_1 = -1;
                    int ped_2 = -1;
                    int adc_1 = -1;
                    int adc_2 = -1;
                    int tdc_1 = -1;
                    int tdc_2 = -1;
                    int adc_idx1 = -1;
                    int adc_idx2 = -1;
                    int tdc_idx1 = -1;
                    int tdc_idx2 = -1;

                    for (BaseHit h : hitlists.get(i)) {
                        if (h.get_ADC1() > 0) {
                            adc_1 = h.get_ADC1();
                            adc_idx1 = h.ADCbankHitIdx1;
                            if (h.get_ADCTime1() > 0) {
                                t_1 = h.get_ADCTime1();
                            }
                            if (h.get_ADCpedestal1() > 0) {
                                ped_1 = h.get_ADCpedestal1();
                            }
                        }
                        if (h.get_ADC2() > 0) {
                            adc_2 = h.get_ADC2();
                            adc_idx2 = h.ADCbankHitIdx2;
                            if (h.get_ADCTime2() > 0) {
                                t_2 = h.get_ADCTime2();
                            }
                            if (h.get_ADCpedestal2() > 0) {
                                ped_2 = h.get_ADCpedestal2();
                            }
                        }
                        if (h.get_TDC1() > 0) {
                            tdc_1 = h.get_TDC1();
                            tdc_idx1 = h.TDCbankHitIdx1;
                        }
                        if (h.get_TDC2() > 0) {
                            tdc_2 = h.get_TDC2();
                            tdc_idx2 = h.TDCbankHitIdx2;
                        }
                    }
                    hit.ADC1 = adc_1;
                    hit.ADC2 = adc_2;
                    hit.TDC1 = tdc_1;
                    hit.TDC2 = tdc_2;
                    hit.ADCpedestal1 = ped_1;
                    hit.ADCpedestal2 = ped_2;
                    hit.ADCTime1 = t_1;
                    hit.ADCTime2 = t_2;
                    hit.ADCbankHitIdx1 = adc_idx1;
                    hit.ADCbankHitIdx2 = adc_idx2;
                    hit.TDCbankHitIdx1 = tdc_idx1;
                    hit.TDCbankHitIdx2 = tdc_idx2;

                    matchLists.add(hit);
                    // System.out.println(i+")  s "+hit.get_Sector()+" l "+hit.get_Layer()+" c "+hit.get_Component()+" adcL "+hit.get_ADC1()+" adcR "+hit.get_ADC2()+" tdcL "+
                    // hit.get_TDC1()+" tdcR "+hit.get_TDC2());

                }
            }

        }

        return matchLists;
    }

}

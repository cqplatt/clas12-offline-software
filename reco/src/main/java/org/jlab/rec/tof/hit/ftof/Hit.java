/**
 * 
 */
package org.jlab.rec.tof.hit.ftof;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.detector.geant4.v2.FTOFGeant4Factory;
//import org.jlab.detector.geant4.v2.FTOFGeant4Factory;
import org.jlab.detector.hits.DetHit;
import org.jlab.detector.hits.FTOFDetHit;
import org.jlab.detector.volume.G4Box;
//import org.jlab.geom.component.ScintillatorMesh;
//import org.jlab.geom.detector.ftof.FTOFDetectorMesh;
//import org.jlab.geom.detector.ftof.FTOFFactory;
import org.jlab.geom.prim.Line3D;
import org.jlab.geom.prim.Path3D;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Vector3D;
import org.jlab.geometry.prim.Line3d;
import org.jlab.rec.ftof.CCDBConstantsLoader;
import org.jlab.rec.ftof.Constants;
import org.jlab.rec.tof.banks.ftof.HitReader;
import org.jlab.rec.tof.hit.AHit;
import org.jlab.rec.tof.hit.IGetCalibrationParams;
import org.jlab.service.ftof.FTOFEngine;

import eu.mihosoft.vrl.v3d.Vector3d;

/**
 * @author ziegler
 *
 */
public class Hit extends AHit implements IGetCalibrationParams {

	/**
	 * IMPORTANT:
	 * A possible mismatch between the evaluation at the face of the counter from tracking and in the middle of 
     * the counter from clustering needs to be checked in the geometry package !!!
	 */
	public Hit(int id, int panel, int sector, int paddle, int aDCL, int tDCL, int aDCR, int tDCR)  {
		super(id, panel, sector, paddle, aDCL, tDCL, aDCR, tDCR);
		
	}
	
	private Line3D 	_paddleLine;		  // paddle line 
	private FTOFDetHit _matchedTrackHit;  // matched hit information from tracking; this contains the information of the entrance and exit point of the track with the FTOF hit counter
	private Line3d _matchedTrack;
	public int _AssociatedTrkId = -1;
	
	public Line3D get_paddleLine() {
		return _paddleLine;
	}
	
	public void set_paddleLine(Line3D paddleLine) {
		this._paddleLine = paddleLine;
	}

	public FTOFDetHit get_matchedTrackHit() {
		return _matchedTrackHit;
	}

	public void set_matchedTrackHit(FTOFDetHit matchedTrackHit) {
		this._matchedTrackHit = matchedTrackHit;
	}

	public Line3d get_matchedTrack() {
		return _matchedTrack;
	}

	public void set_matchedTrack(Line3d _matchedTrack) {
		this._matchedTrack = _matchedTrack;
	}

	public void set_HitParameters(int superlayer) {
				
		double pl = this.get_paddleLine().length();
		
		// Get all the constants used in the hit parameters calculation
		double TW0L = this.TW01();
		double TW0R = this.TW02();
		double TW1L = this.TW11();
		double TW1R = this.TW12();
		double lambdaL = this.lambda1();
		this.set_lambda1(lambdaL);
		this.set_lambda1Unc(this.lambda1Unc());
		double lambdaR = this.lambda1();
		this.set_lambda2(lambdaR);
		this.set_lambda2Unc(this.lambda2Unc());
		double yOffset = this.yOffset();		
		double vL = this.v1();
		double vR = this.v2();
		double vLUnc = this.v1Unc();
		double vRUnc = this.v2Unc();
		double PEDL = this.PED1();
		double PEDR = this.PED2();
		double PEDLUnc = this.PED1Unc();
		double PEDRUnc = this.PED2Unc();
		double paddle2paddle = this.PaddleToPaddle();
		double timeOffset = this.TimeOffset();
		double LSBConv = this.LSBConversion();
		double LSBConvErr = this.LSBConversionUnc();
		double ADCLErr = this.ADC1Unc();
		double ADCRErr = this.ADC2Unc();
		double TDCLErr = this.TDC1Unc();
		double TDCRErr = this.TDC2Unc();
		double ADC_MIP = this.ADC_MIP();
		double ADC_MIPErr = this.ADC_MIPUnc();
		double DEDX_MIP = this.DEDX_MIP();
		double ScinBarThickn = this.ScinBarThickn();
		
		this.set_HitParams(superlayer, TW0L, TW0R, TW1L, TW1R, lambdaL, lambdaR, yOffset, vL, vR, vLUnc, vRUnc, PEDL, PEDR, PEDLUnc, PEDRUnc, paddle2paddle, timeOffset, LSBConv, LSBConvErr, ADCLErr, ADCRErr, TDCLErr, TDCRErr, ADC_MIP, ADC_MIPErr, DEDX_MIP, ScinBarThickn, pl);
		// Set the hit position in the local coordinate of the bar
		this.set_Position(this.calc_hitPosition());

	}
	
	 public void setPaddleLine(FTOFGeant4Factory geometry) {
	// get the line in the middle of the paddle
		G4Box comp =(G4Box)geometry.getComponent(this.get_Sector(), this.get_Panel(), this.get_Paddle());
		Line3D paddleLine = new Line3D();
		// The scintilator paddles are constructed with the length of the paddle
        // as X dimention in the lab frame, so getLineX will return a line going
        // through the center of the paddle and length() equal to the paddle length
		paddleLine.set(comp.getLineX().origin().x, comp.getLineX().origin().y, comp.getLineX().origin().z, comp.getLineX().end().x, comp.getLineX().end().y, comp.getLineX().end().z);
		this.set_paddleLine(paddleLine);
	 }	

	public Point3D calc_hitPosition() {
		Point3D hitPosition = new Point3D();
        Vector3D dir = new Vector3D(
                this.get_paddleLine().end().x() - this.get_paddleLine().origin().x(),
                this.get_paddleLine().end().y() - this.get_paddleLine().origin().y(),
                this.get_paddleLine().end().z() - this.get_paddleLine().origin().z()
        );
        dir.unit();  
        Point3D startpoint = this.get_paddleLine().origin();
        double L_2 = this.get_paddleLine().length()/2;
        hitPosition.setX(startpoint.x() + (L_2+this.get_y())*dir.x());
        hitPosition.setY(startpoint.y() + (L_2+this.get_y())*dir.y());
        hitPosition.setZ(startpoint.z() + (L_2+this.get_y())*dir.z());

        return hitPosition;
        
	}
	 
	public void printInfo() {
		DecimalFormat form = new DecimalFormat("#.##");
		String s = " FTOF Hit in "+" Sector "+this.get_Sector()+" Panel "+this.get_Panel()+" Paddle "+this.get_Paddle()+" with Status "+this.get_StatusWord()+" in Cluster "+this.get_AssociatedClusterID()+" : \n"+
				"  ADCL =  "+this.get_ADC1()+ 
				"  ADCR =  "+this.get_ADC2()+ 
				"  TDCL =  "+this.get_TDC1()+ 
				"  TDCR =  "+this.get_TDC2()+ 
				"\n  tL =  "+form.format(this.get_t1())+ 
				"  tR =  "+form.format(this.get_t2())+ 
				"  t =  "+form.format(this.get_t())+ 
				"  timeWalkL =  "+form.format(this.get_timeWalk1())+ 
				"  timeWalkR =  "+form.format(this.get_timeWalk2())+ 
				"  lambdaL =  "+form.format(this.get_lambda1())+ 
				"  lambdaR =  "+form.format(this.get_lambda2())+ 
				"  Energy =  "+form.format(this.get_Energy())+ 
				"  EnergyL =  "+form.format(this.get_Energy1())+ 
				"  EnergyR =  "+form.format(this.get_Energy2())+ 
				"  y =  "+form.format(this.get_y())+"\n ";
		if(this.get_Position()!=null)  {
			s+=	"  xPos =  "+form.format(this.get_Position().x())+
				"  yPos =  "+form.format(this.get_Position().y())+
				"  zPos =  "+form.format(this.get_Position().z())+
				"\n ";
		}
		System.out.println(s);
	}


	@Override
	public int compareTo(AHit arg) {
		// Sort by sector, panel, paddle
		int return_val = 0 ;
		int CompSec = this.get_Sector() < arg.get_Sector()  ? -1 : this.get_Sector()  == arg.get_Sector()  ? 0 : 1;
		int CompPan = this.get_Panel()  < arg.get_Panel()   ? -1 : this.get_Panel()   == arg.get_Panel()   ? 0 : 1;
		int CompPad = this.get_Paddle() < arg.get_Paddle()  ? -1 : this.get_Paddle()  == arg.get_Paddle()  ? 0 : 1;
		
		int return_val1 = ((CompPan ==0) ? CompPad : CompPan); 
		return_val = ((CompSec ==0) ? return_val1 : CompSec);
		
		return return_val;
	}
	

	@Override
	public double TW01() {
		double TW0L = CCDBConstantsLoader.TW0L[this.get_Sector()-1][this.get_Panel()-1][this.get_Paddle()-1];
		
		return TW0L;
	}


	@Override
	public double TW02() {
		double TW0R = CCDBConstantsLoader.TW0R[this.get_Sector()-1][this.get_Panel()-1][this.get_Paddle()-1];
		
		return TW0R;
	}


	@Override
	public double TW11() {
		double TW1L = CCDBConstantsLoader.TW1L[this.get_Sector()-1][this.get_Panel()-1][this.get_Paddle()-1];
		
		return TW1L;
	}


	@Override
	public double TW12() {
		double TW1R = CCDBConstantsLoader.TW1R[this.get_Sector()-1][this.get_Panel()-1][this.get_Paddle()-1];
		
		return TW1R;
	}


	@Override
	public double lambda1() {
		return CCDBConstantsLoader.LAMBDAL[this.get_Sector()-1][this.get_Panel()-1][this.get_Paddle()-1];
	}


	@Override
	public double lambda2() {
		return CCDBConstantsLoader.LAMBDAR[this.get_Sector()-1][this.get_Panel()-1][this.get_Paddle()-1];
	}


	@Override
	public double lambda1Unc() {
		return CCDBConstantsLoader.LAMBDALU[this.get_Sector()-1][this.get_Panel()-1][this.get_Paddle()-1];
	}


	@Override
	public double lambda2Unc() {
		return CCDBConstantsLoader.LAMBDARU[this.get_Sector()-1][this.get_Panel()-1][this.get_Paddle()-1];
	}


	@Override
	public double yOffset() {
		return CCDBConstantsLoader.YOFF[this.get_Sector()-1][this.get_Panel()-1][this.get_Paddle()-1];
	}


	@Override
	public double v1() {
		return CCDBConstantsLoader.EFFVELL[this.get_Sector()-1][this.get_Panel()-1][this.get_Paddle()-1];
	}


	@Override
	public double v2() {
		return CCDBConstantsLoader.EFFVELR[this.get_Sector()-1][this.get_Panel()-1][this.get_Paddle()-1];
	}


	@Override
	public double v1Unc() {
		return CCDBConstantsLoader.EFFVELLU[this.get_Sector()-1][this.get_Panel()-1][this.get_Paddle()-1];
	}


	@Override
	public double v2Unc() {
		return CCDBConstantsLoader.EFFVELRU[this.get_Sector()-1][this.get_Panel()-1][this.get_Paddle()-1];
	}


	@Override
	public double PED1() {
		return Constants.PEDL[this.get_Panel()-1];
	}

	@Override
	public double PED2() {
		return Constants.PEDR[this.get_Panel()-1];
	}
	
	@Override
	public double PED1Unc() {
		return Constants.PEDLUNC[this.get_Panel()-1];
	}

	@Override
	public double PED2Unc() {
		return Constants.PEDRUNC[this.get_Panel()-1];
	}

	@Override
	public double ADC1Unc() {
		return Constants.ADCJITTERL;
	}
	
	@Override
	public double TDC2Unc() {
		return Constants.TDCJITTERR;
	}
	
	@Override
	public double TDC1Unc() {
		return Constants.TDCJITTERL;
	}
	
	@Override
	public double ADC2Unc() {
		return Constants.ADCJITTERR;
	}
	@Override
	public double PaddleToPaddle(){
		return CCDBConstantsLoader.PADDLE2PADDLE[this.get_Sector()-1][this.get_Panel()-1][this.get_Paddle()-1];	
	}
	
	@Override
	public double TimeOffset() {
		return CCDBConstantsLoader.LR[this.get_Sector()-1][this.get_Panel()-1][this.get_Paddle()-1];
	}
	
	@Override
	public double LSBConversion() {
		return Constants.LSBCONVFAC;
	}
	
	@Override
	public double LSBConversionUnc() {
		return Constants.LSBCONVFACERROR;
	}
	
	@Override
	public double ADC_MIP() {
		//return Constants.ADC_MIP[this.get_Panel()-1];
		return CCDBConstantsLoader.MIPL[this.get_Sector()-1][this.get_Panel()-1][this.get_Paddle()-1];
	}
	
	@Override
	public double ADC_MIPUnc() {
		//return Constants.ADC_MIP_UNC[this.get_Panel()-1];
		return CCDBConstantsLoader.MIPLU[this.get_Sector()-1][this.get_Panel()-1][this.get_Paddle()-1];
	}
	
	@Override
	public double DEDX_MIP() {
		return Constants.DEDX_MIP;
	}
	
	@Override
	public double ScinBarThickn() {
		return Constants.SCBARTHICKN[this.get_Panel()-1];
	}
	
	
	public static void main (String arg[]) throws IOException{

		FTOFEngine rec = new FTOFEngine() ;		
		rec.init();
		HitReader hrd = new HitReader();
		
		// get the status
		int id = 1;
		int sector = 4;
		int paddle = 21;
		// set the superlayer to get the paddle position from the geometry package
		int superlayer = 1;
		List<ArrayList<Path3D>> trks = null;
		List<double[]> paths = null;
		//Detector geometry = rec.getGeometry("FTOF");	
	    DatabaseConstantProvider provider = new DatabaseConstantProvider(10,"default");
	    
        provider.loadTable("/geometry/ftof/panel1a/paddles");        
        provider.loadTable("/geometry/ftof/panel1a/panel");
        provider.loadTable("/geometry/ftof/panel1b/paddles");
        provider.loadTable("/geometry/ftof/panel1b/panel");
        provider.loadTable("/geometry/ftof/panel2/paddles");
        provider.loadTable("/geometry/ftof/panel2/panel");
	    //disconncect from database. Important to do this after loading tables.
	    provider.disconnect(); 

	    FTOFGeant4Factory factory = new FTOFGeant4Factory(provider);


		int statusL = CCDBConstantsLoader.STATUSL[sector-1][0][paddle-1];
		int statusR = CCDBConstantsLoader.STATUSR[sector-1][0][paddle-1];
		
		Random rnd = new Random();

		for(int itrack=0; itrack<1000; itrack++){
			Line3d line = new Line3d(new Vector3d(rnd.nextDouble() * 10000 - 5000, rnd.nextDouble() * 10000 - 5000,  3000),
								new Vector3d(rnd.nextDouble() * 10000 - 5000, rnd.nextDouble() * 10000 - 5000,  9000));

			List<DetHit> hits = factory.getIntersections(line);

			for(DetHit hit: hits){
				FTOFDetHit fhit = new FTOFDetHit(hit);
				System.out.println( "\t"+fhit.length());
				System.out.println( "\t\t"+fhit.getSector());
				System.out.println( "\t\t"+fhit.getLayer());
				System.out.println( "\t\t"+fhit.getPaddle());
				System.out.println( "\t\tentry: "+fhit.origin());
				System.out.println( "\t\texit:  "+fhit.end());
				System.out.println( "\t\tmid:   "+fhit.mid());
				System.out.println( "\t\tlength: "+fhit.length());
			}
		}
		// create the hit object
		//Hit hit = new Hit(id, 1, sector, paddle, 900, 900, 800, 1000) ;
		//String statusWord = hrd.set_StatusWord(statusL, statusR, hit.get_ADC1(), hit.get_TDC1(), hit.get_ADC2(), hit.get_TDC2());
		 
		//System.out.println(statusWord);
		
		//hit.set_StatusWord(statusWord);
		//hit.set_HitParameters(superlayer, geometry, trks, paths);
		// read the hit object
		//System.out.println(" hit "); hit.printInfo();
		
	}


}

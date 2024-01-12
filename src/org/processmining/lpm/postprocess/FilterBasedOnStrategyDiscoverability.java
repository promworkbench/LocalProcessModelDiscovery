package org.processmining.lpm.postprocess;

import java.util.ArrayList;
import java.util.List;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpm.dialogs.UtilityLocalProcessModelParameters.PruningStrategies;
import org.processmining.lpm.util.LocalProcessModelRanking;
import org.processmining.lpm.util.UtilityLocalProcessModel;

@Plugin(
		name = "Filter Utility Local Process Model Ranking by Strategy Discoverability", 
		parameterLabels = {"Utility Local Process Model Ranking"}, 
	    returnLabels = {"Filtered Utility Local Process Model Ranking"}, 
	    returnTypes = { LocalProcessModelRanking.class }
		)
public class FilterBasedOnStrategyDiscoverability {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "N. Tax", email = "n.tax@tue.nl")
	@PluginVariant(variantLabel = "Filter Utility Local Process Model Ranking by Strategy Discoverability", requiredParameterLabels = {0})
	public LocalProcessModelRanking run(PluginContext context, LocalProcessModelRanking lpmr){
		PruningStrategies strategy = PruningStrategies.PREVIOUS_LEQ;
		int k = 1;
		return filterRanking(lpmr, strategy, k);
	}
	
	public static boolean expandModel(UtilityLocalProcessModel lpm, PruningStrategies strat, int k){
		int count = 0;
		List<Double> utilityList = new ArrayList<Double>(lpm.getUtilityList());
		utilityList.add(lpm.getUtility());
		double bestUtility = utilityList.get(0);
		boolean expand = true;
		switch(strat){
			case PREVIOUS_LEQ:
				for(int i=0; i<utilityList.size()-1;i++){
					if(utilityList.get(i+1) <= utilityList.get(i))
						count++;
					else
						count=0;
					expand = expand && (count < k);
					if(!expand)
						break;
				}
				return expand;
			case PREVIOUS_L:
				for(int i=0; i<utilityList.size()-1;i++){
					if(utilityList.get(i+1) < utilityList.get(i))
						count++;
					else
						count=0;
					expand = expand && (count < k);
					if(!expand)
						break;
				}
				return expand;
			case BEST_LEQ: 
				for(int i=0; i<utilityList.size()-1;i++){
					if(utilityList.get(i+1) <= bestUtility)
						count++;
					else{
						count=0;
						bestUtility = utilityList.get(i+1);
					}
					expand = expand && (count < k);
					if(!expand)
						break;
				}
				return expand;
			case BEST_L:
				for(int i=0; i<utilityList.size()-1;i++){
					if(utilityList.get(i+1) < bestUtility)
						count++;
					else{
						count=0;
						bestUtility = utilityList.get(i+1);
					}
					expand = expand && (count < k);
					if(!expand)
						break;
				}
				return expand;
			case NONE :
				return true;
			default :
				System.err.println("Default case should not be executed");
				return true;
		}
	}
	
	public static LocalProcessModelRanking filterRanking(LocalProcessModelRanking lpmr, PruningStrategies strat, int k)
	{
		LocalProcessModelRanking rank = new LocalProcessModelRanking();

		boolean bool = false;
		int iter = 0;
		int count = 0;
		UtilityLocalProcessModel sapn;
		Double bestAncestor= 0d;

		switch(strat)
		{
			case PREVIOUS_LEQ:
				for(iter = 0; iter< lpmr.getSize();iter++)
				{
					if(!(lpmr.getNet(iter) instanceof UtilityLocalProcessModel))
					{
						System.err.println("error, lpmr is not a utility lpm");
						break;
					}

					sapn = (UtilityLocalProcessModel) lpmr.getNet(iter);
					bool = false;
					if(sapn.getUtilityList().size()>=k+1)
					{
						bool = false;
						for (int i=0; i<sapn.getUtilityList().size()-k;i++)
						{
							count = 0;
							for(int j=i+1;j<=i+k;j++)
							{
								if(sapn.getUtilityList().get(j) <= sapn.getUtilityList().get(j-1))
									count++;
							}
							bool = bool || (count >= k);
						}	
					}
					if(!bool)
					{
						rank.addElement(sapn);
					}
				}
				break;
			case PREVIOUS_L:
				for(iter = 0; iter< lpmr.getSize();iter++)
				{
					if(!(lpmr.getNet(iter) instanceof UtilityLocalProcessModel))
					{
						System.err.println("error, lpmr is not a utility lpm");
						break;
					}

					sapn = (UtilityLocalProcessModel) lpmr.getNet(iter);
					bool = false;

					if(sapn.getUtilityList().size()>=k+1)
					{
						bool = false;
						for (int i=0; i<sapn.getUtilityList().size()-k;i++)
						{
							count = 0;
							for(int j=i+1;j<=i+k;j++)
							{
								if(sapn.getUtilityList().get(j) < sapn.getUtilityList().get(j-1))
									count++;
							}
							bool = bool || (count >= k);
						}	
					}
					if(!bool)
					{
						rank.addElement(sapn);
					}
				}
				break;
			case BEST_LEQ: 
				for(iter = 0; iter< lpmr.getSize();iter++)
				{
					if(!(lpmr.getNet(iter) instanceof UtilityLocalProcessModel))
					{
						System.err.println("error, lpmr is not a utility lpm");
						break;
					}

					sapn = (UtilityLocalProcessModel) lpmr.getNet(iter);
					bool = false;

					if(sapn.getUtilityList().size()>=k+1)
					{
						bool = false;
						for (int i=0; i<sapn.getUtilityList().size()-k;i++)
						{
							bestAncestor = sapn.getUtilityList().get(i);
							count = 0;

							for(int j=i+1;j<=i+k;j++)
							{
								if(sapn.getUtilityList().get(j) <= bestAncestor)
								{
									count++;
									if(count >= k)
										bool = bool || (count >= k);
								}	
							}
						}
					}
					if(!bool)
					{
						rank.addElement(sapn);
					}
				}
				break;
			case BEST_L:
				for(iter = 0; iter< lpmr.getSize();iter++)
				{
					if(!(lpmr.getNet(iter) instanceof UtilityLocalProcessModel))
					{
						System.err.println("error, lpmr is not a utility lpm");
						break;
					}

					sapn = (UtilityLocalProcessModel) lpmr.getNet(iter);
					bool = false;

					if(sapn.getUtilityList().size()>=k+1)
					{
						bool = false;
						for (int i=0; i<sapn.getUtilityList().size()-k;i++)
						{
							bestAncestor = sapn.getUtilityList().get(i);
							count = 0;

							for(int j=i+1;j<=i+k;j++)
							{
								if(sapn.getUtilityList().get(j) < bestAncestor)
								{
									count++;
									if(count >= k)
										bool = bool || (count >= k);
								}	
							}
						}
					}
					if(!bool)
					{
						rank.addElement(sapn);
					}
				}
				break;
			case NONE :
				return lpmr;
			default :
				return lpmr;
		}
		return rank;
	}
}

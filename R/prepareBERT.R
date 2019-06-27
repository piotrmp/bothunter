library(moments)

prepareBERT=function(d1,d2){
	users=unique(d1$ID1)
	result=data.frame(user=users)
	result['y']=NA
	result['B_means']=NA
	result['B_medians']=NA
	result['B_sds']=NA
	result['B_ups']=NA
	result['B_downs']=NA
	result['B_skewness']=NA
	result['B_kurtosis']=NA
	for (user in users){
		result$y[result$user==user]=mean(d1$Quality[d1$ID1==user])
		sims=d2[d1$ID1==user,2]
		result$B_means[result$user==user]=mean(sims)
		result$B_medians[result$user==user]=median(sims)
		result$B_sds[result$user==user]=sd(sims)
		result$B_ups[result$user==user]=mean(sims>0.9)
		result$B_downs[result$user==user]=mean(sims<0.1)
		result$B_skewness[result$user==user]=skewness(sims)
		result$B_kurtosis[result$user==user]=kurtosis(sims)
	}
	result$B_skewness[is.nan(result$B_skewness)]=0
	result$B_kurtosis[is.nan(result$B_kurtosis)]=100
	return(result)
}



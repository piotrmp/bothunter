library(e1071)
library(glmnet)
library(SparseM)
library(Matrix)
source('bic.R')
library(foreach)
library(doMC)
#registerDoMC(4)

prepare=function(users,trainPred,userTypes){
  result=data.frame(user=users)
  result['y']=NA
  result['G_means']=NA
  result['G_medians']=NA
  result['G_sds']=NA
  result['G_ups']=NA
  result['G_downs']=NA
  result['G_skewness']=NA
  result['G_kurtosis']=NA
  for (user in 1:length(users)){
    result$y[user]=userTypes[userTypes$V1==as.character(users[user]),'V2']
    sims=trainPred[user,1:100]
    sims=sims[!is.na(sims)]
    result$G_means[user]=mean(sims)
    result$G_medians[user]=median(sims)
    result$G_sds[user]=sd(sims)
    result$G_ups[user]=mean(sims>0.9)
    result$G_downs[user]=mean(sims<0.1)
    result$G_skewness[user]=skewness(sims)
    result$G_kurtosis[user]=kurtosis(sims)
  }
  result$G_skewness[is.nan(result$G_skewness)]=0
  result$G_kurtosis[is.nan(result$G_kurtosis)]=100
  return(result)
}

prepareGLM=function(userTypes,trainD,trainS,testD,testS){
  trainU=trainD$ID
  train=cbind(Matrix(as.matrix(trainD[,-1]),sparse=T),trainS)
  testU=testD$ID
  test=cbind(Matrix(as.matrix(testD[,-1]),sparse=T),testS)
  users=unique(trainU)
  trainPred=foreach (i=1:length(users),.combine=rbind)%dopar%{
    set.seed(3+i)
    user=users[i]
    selection=rep(FALSE,nrow(train))
    selection[sample(nrow(train),100*1)]=TRUE
    selection[trainU==user]=TRUE
    cat(i,"/",length(users),"\n")
    x=train[selection,]
    y=1*(trainU[selection]==user)
    model=glmnet(x,y,family="binomial")
    lambda=select_lambda(model,y,x,"BIC")
    pred=predict(model,train[trainU==user,],type="response",s=lambda)
    length(pred)=100
    t(pred)
  }
  trainSummary=prepare(users,trainPred,userTypes)
  
  users=unique(testU)
  testPred=foreach (i=1:length(users),.combine=rbind)%dopar%{
    set.seed(3+i)
    user=users[i]
    selection=rep(FALSE,nrow(train))
    selection[sample(nrow(train),100*1)]=TRUE
    cat(i,"/",length(users),"\n")
    x=rbind(train[selection,],test[testU==user,])
    y=c(rep(0,sum(selection)),rep(1,sum(testU==user)))
    model=glmnet(x,y,family="binomial")
    lambda=select_lambda(model,y,x,"BIC")
    pred=predict(model,test[testU==user,],type="response",s=lambda)
    length(pred)=100
    t(pred)
  }
  testSummary=prepare(users,testPred,userTypes)
  return(list(train=trainSummary,test=testSummary))
}

prepareGLMtrain=function(userTypes,trainD,trainS){
  trainU=trainD$ID
  train=cbind(Matrix(as.matrix(trainD[,-1]),sparse=T),trainS)
  users=unique(trainU)
  trainPred=foreach (i=1:length(users),.combine=rbind)%dopar%{
    set.seed(3+i)
    user=users[i]
    selection=rep(FALSE,nrow(train))
    selection[sample(nrow(train),100*1)]=TRUE
    selection[trainU==user]=TRUE
    cat(i,"/",length(users),"\n")
    x=train[selection,]
    y=1*(trainU[selection]==user)
    model=glmnet(x,y,family="binomial")
    lambda=select_lambda(model,y,x,"BIC")
    pred=predict(model,train[trainU==user,],type="response",s=lambda)
    length(pred)=100
    t(pred)
  }
  trainSummary=prepare(users,trainPred,userTypes)
  
  return(trainSummary)
}

prepareGLMtest=function(userTypes,trainD,trainS,testD,testS){
  trainU=trainD$ID
  train=cbind(Matrix(as.matrix(trainD[,-1]),sparse=T),trainS)
  testU=testD$ID
  test=cbind(Matrix(as.matrix(testD[,-1]),sparse=T),testS)

  users=unique(testU)
  testPred=foreach (i=1:length(users),.combine=rbind)%dopar%{
    set.seed(3+i)
    user=users[i]
    selection=rep(FALSE,nrow(train))
    available=which(as.character(trainU)!=as.character(user))
    selection[sample(available,100*1)]=TRUE
    cat(i,"/",length(users),"\n")
    x=rbind(train[selection,],test[testU==user,])
    y=c(rep(0,sum(selection)),rep(1,sum(testU==user)))
    model=glmnet(x,y,family="binomial")
    lambda=select_lambda(model,y,x,"BIC")
    pred=predict(model,test[testU==user,],type="response",s=lambda)
    length(pred)=100
    t(pred)
  }
  testSummary=prepare(users,testPred,userTypes)
  return(testSummary)
}


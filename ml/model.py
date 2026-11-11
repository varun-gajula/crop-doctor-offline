"""
Crop Doctor - Vision Model Architectures & Temperature Calibration
-----------------------------------------------------------------
Supports MobileNetV3-Large (primary Arm target) and ConvNeXt-Tiny,
plus post-hoc Temperature Scaling for probability calibration.
"""

import torch
import torch.nn as nn
import torchvision.models as models

def create_model(model_name="mobilenet_v3_large", num_classes=25, pretrained=True):
    """
    Initializes vision backbone and modifies head for 25 target classes.
    """
    if model_name == "mobilenet_v3_large":
        weights = models.MobileNet_V3_Large_Weights.DEFAULT if pretrained else None
        model = models.mobilenet_v3_large(weights=weights)
        in_features = model.classifier[3].in_features
        model.classifier[3] = nn.Linear(in_features, num_classes)
        return model

    elif model_name == "convnext_tiny":
        weights = models.ConvNeXt_Tiny_Weights.DEFAULT if pretrained else None
        model = models.convnext_tiny(weights=weights)
        in_features = model.classifier[2].in_features
        model.classifier[2] = nn.Linear(in_features, num_classes)
        return model

    else:
        raise ValueError(f"Unsupported model architecture: {model_name}")

class TemperatureScaler(nn.Module):
    """
    Calibrates confidence scores using a learned temperature scalar T:
    P(y = i) = softmax(logits / T)_i
    A well-calibrated model ensures 90% confidence actually means 90% accuracy.
    """
    def __init__(self):
        super().__init__()
        self.temperature = nn.Parameter(torch.ones(1) * 1.5)

    def forward(self, logits):
        return logits / self.temperature

    def fit(self, val_loader, model, device):
        """
        Tunes temperature parameter on validation logits to minimize Negative Log Likelihood.
        """
        self.to(device)
        model.eval()
        logits_list = []
        labels_list = []

        with torch.no_grad():
            for imgs, labels, _ in val_loader:
                imgs = imgs.to(device)
                logits = model(imgs)
                logits_list.append(logits)
                labels_list.append(labels.to(device))

        logits = torch.cat(logits_list)
        labels = torch.cat(labels_list)

        nll_criterion = nn.CrossEntropyLoss()
        optimizer = torch.optim.LBFGS([self.temperature], lr=0.01, max_iter=50)

        def eval_loss():
            optimizer.zero_grad()
            loss = nll_criterion(self.forward(logits), labels)
            loss.backward()
            return loss

        optimizer.step(eval_loss)
        return float(self.temperature.item())


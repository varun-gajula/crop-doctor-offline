"""
Crop Doctor - Vision Model Architectures & Temperature Calibration
-----------------------------------------------------------------
Supports MobileNetV3-Large (primary Arm target) and ConvNeXt-Tiny,
plus post-hoc Temperature Scaling for probability calibration.
"""

import torch
import torch.nn as nn
import torchvision.models as models

class MultiTaskCropVisionModel(nn.Module):
    """
    Multi-Task Vision Model for Arm SoC:
    Simultaneously outputs:
    1. Disease / Pest Classification (25 target classes)
    2. Visual Severity Level Classification (0: Mild, 1: Moderate, 2: Severe)
    """
    def __init__(self, backbone_name="mobilenet_v3_large", num_classes=25, num_severity=3, pretrained=True):
        super().__init__()
        self.backbone_name = backbone_name
        self.num_classes = num_classes
        self.num_severity = num_severity

        if backbone_name == "mobilenet_v3_large":
            weights = models.MobileNet_V3_Large_Weights.DEFAULT if pretrained else None
            base = models.mobilenet_v3_large(weights=weights)
            self.features = base.features
            self.avgpool = base.avgpool
            in_features = base.classifier[0].in_features  # 960

            self.classifier_head = nn.Sequential(
                nn.Linear(in_features, 512),
                nn.Hardswish(),
                nn.Dropout(p=0.2),
                nn.Linear(512, num_classes)
            )
            self.severity_head = nn.Sequential(
                nn.Linear(in_features, 256),
                nn.Hardswish(),
                nn.Dropout(p=0.2),
                nn.Linear(256, num_severity)
            )

        elif backbone_name == "efficientnet_b0":
            weights = models.EfficientNet_B0_Weights.DEFAULT if pretrained else None
            base = models.efficientnet_b0(weights=weights)
            self.features = base.features
            self.avgpool = base.avgpool
            in_features = base.classifier[1].in_features  # 1280

            self.classifier_head = nn.Sequential(
                nn.Dropout(p=0.2),
                nn.Linear(in_features, num_classes)
            )
            self.severity_head = nn.Sequential(
                nn.Dropout(p=0.2),
                nn.Linear(in_features, num_severity)
            )
        else:
            raise ValueError(f"Unsupported multi-task backbone: {backbone_name}")

    def forward(self, x):
        feat = self.features(x)
        feat = self.avgpool(feat)
        feat = torch.flatten(feat, 1)

        class_logits = self.classifier_head(feat)
        severity_logits = self.severity_head(feat)
        return class_logits, severity_logits

def create_model(model_name="mobilenet_v3_large", num_classes=25, num_severity=3, multi_task=False, pretrained=True):
    """
    Initializes vision backbone. Supports single-task or multi-task (disease + severity) heads.
    """
    if multi_task:
        return MultiTaskCropVisionModel(model_name, num_classes=num_classes, num_severity=num_severity, pretrained=pretrained)

    if model_name == "mobilenet_v3_large":
        weights = models.MobileNet_V3_Large_Weights.DEFAULT if pretrained else None
        model = models.mobilenet_v3_large(weights=weights)
        in_features = model.classifier[3].in_features
        model.classifier[3] = nn.Linear(in_features, num_classes)
        return model

    elif model_name == "efficientnet_b0":
        weights = models.EfficientNet_B0_Weights.DEFAULT if pretrained else None
        model = models.efficientnet_b0(weights=weights)
        in_features = model.classifier[1].in_features
        model.classifier[1] = nn.Linear(in_features, num_classes)
        return model

    elif model_name == "convnext_tiny":
        weights = models.ConvNeXt_Tiny_Weights.DEFAULT if pretrained else None
        model = models.convnext_tiny(weights=weights)
        in_features = model.classifier[2].in_features
        model.classifier[2] = nn.Linear(in_features, num_classes)
        return model

    else:
        raise ValueError(f"Unsupported model architecture: {model_name}")

class EnergyOODDetector:
    """
    Energy-based Out-Of-Distribution (OOD) Detector (Rule H11):
    Computes free energy E(x; T) = -T * log(sum(exp(logits / T)))
    Rejects non-crop images (hands, desks, shoes) that exhibit high energy / low calibrated confidence.
    """
    def __init__(self, temperature=1.0, energy_threshold=-10.0):
        self.temperature = temperature
        self.energy_threshold = energy_threshold

    def compute_energy(self, logits):
        # logits shape: [batch, num_classes]
        scaled = logits / self.temperature
        return -self.temperature * torch.logsumexp(scaled, dim=-1)

    def is_in_distribution(self, logits):
        energy = self.compute_energy(logits)
        return energy < self.energy_threshold

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

